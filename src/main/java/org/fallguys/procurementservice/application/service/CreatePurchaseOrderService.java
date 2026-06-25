package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.command.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.CreatePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.GeneratePoCodePort;
import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.port.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.domain.exception.*;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;
import org.fallguys.procurementservice.domain.model.purchaseorder.WarehouseRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreatePurchaseOrderService implements CreatePurchaseOrderUseCase {

    private final LoadVendorPort loadVendorPort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadWarehouseInfoPort loadWarehouseInfoPort;
    private final LoadItemPort loadItemPort;
    private final GeneratePoCodePort generatePoCodePort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    private final PublishUserActivityPort publishUserActivityPort;

    /**
     * 발주를 생성한다. (DRAFT: 임시 저장 / APPROVED: 즉시 승인)
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 비즈니스 검증: 품목 코드 중복 없음.
     * 3) 공급사 조회: 미존재 시 404.
     * 4) 창고 조회: 미존재·비활성 시 404/400.
     * 5) APPROVED인 경우 품목 서비스 호출로 SKU 존재 검증 후 스냅샷 채워 라인 구성.
     *    DRAFT인 경우 스냅샷 없이 라인 구성.
     * 6) PO 코드 채번 후 도메인 객체 생성.
     * 7) 저장 후 반환.
     *
     * 트랜잭션: 쓰기.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 품목 코드 중복: BusinessValidationException (400)
     * - 공급사·창고 미존재: ResourceNotFoundException (404)
     * - 존재하지 않는 SKU 포함(APPROVED): ResourceNotFoundException (404)
     */
    @Override
    @Transactional
    public PurchaseOrder create(UserRole role, CreatePurchaseOrderCommand command) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }
        validateNoDuplicateItemSkus(command.lines());

        Vendor vendor = loadVendorPort.findActiveByCode(command.vendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND));

        loadWarehousePort.verifyActive(command.warehouseCode());

        String code = generatePoCodePort.generate();
        Instant now = Instant.now();
        boolean isApproved = command.status() == PurchaseOrderStatus.APPROVED;
        List<PurchaseOrderLine> lines = isApproved
                ? buildApprovedLines(command.lines())
                : buildDraftLines(command.lines());

        // 확정(즉시 APPROVED) 생성이면 공급사·창고명을 박제, DRAFT면 code만 보관(조회 시 live 채움).
        VendorRef vendorRef;
        WarehouseRef warehouseRef;
        if (isApproved) {
            WarehouseInfo warehouseInfo = loadWarehouseInfoPort.findByCode(command.warehouseCode());
            vendorRef = VendorRef.snapshot(vendor.getCode(), vendor.getName());
            warehouseRef = WarehouseRef.snapshot(warehouseInfo.code(), warehouseInfo.name());
        } else {
            vendorRef = VendorRef.codeOnly(vendor.getCode());
            warehouseRef = WarehouseRef.codeOnly(command.warehouseCode());
        }

        PurchaseOrder purchaseOrder = PurchaseOrder.create(
                code,
                vendorRef,
                warehouseRef,
                command.status(),
                command.memo(),
                lines,
                command.userCode(),
                now
        );

        PurchaseOrder saved = savePurchaseOrderPort.save(purchaseOrder);

        // 행위자는 불변 사실 → DRAFT 포함 행위 시점에 박제.
        savePurchaseOrderStatusHistoryPort.append(new PurchaseOrderStatusHistory(
                saved.getCode(), command.status(),
                new ActorRef(command.userCode(), command.userName(), command.userPosition()),
                null, now));

        // 사용자 활동: 즉시 APPROVED 생성도 생성 1건으로 본다. 공급사명은 확정(APPROVED) 시에만 박제값 존재.
        // status는 생성 결과 상태 라벨: DRAFT→임시저장, APPROVED→출고대기.
        publishUserActivityPort.publish(new UserActivity(
                command.userCode(), UserActivityType.CREATED,
                saved.getCode(), isApproved ? vendor.getName() : null, now,
                UserActivity.statusLabel(command.status())));

        return saved;
    }

    private void validateNoDuplicateItemSkus(List<PurchaseOrderLineCommand> lines) {
        if (lines == null || lines.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        boolean hasDuplicate = lines.stream()
                .map(PurchaseOrderLineCommand::itemSku)
                .anyMatch(sku -> !seen.add(sku));
        if (hasDuplicate) {
            throw new BusinessValidationException(ProcurementErrorCode.DUPLICATE_ITEM_CODE);
        }
    }

    private List<PurchaseOrderLine> buildDraftLines(List<PurchaseOrderLineCommand> lineCommands) {
        if (lineCommands == null || lineCommands.isEmpty()) return List.of();
        return lineCommands.stream()
                .map(cmd -> {
                    Money unitPrice = Money.of(cmd.unitPrice());
                    return new PurchaseOrderLine(
                            null,
                            cmd.itemSku(),
                            null,
                            null,
                            cmd.quantity(),
                            unitPrice
                    );
                })
                .toList();
    }

    private List<PurchaseOrderLine> buildApprovedLines(List<PurchaseOrderLineCommand> lineCommands) {
        List<String> skus = lineCommands.stream()
                .map(PurchaseOrderLineCommand::itemSku)
                .toList();

        Map<String, ItemInfo> itemInfoMap = loadItemPort.loadAll(skus);

        return lineCommands.stream()
                .map(cmd -> {
                    ItemInfo info = itemInfoMap.get(cmd.itemSku());
                    if (info == null) {
                        throw new ResourceNotFoundException(ProcurementErrorCode.ITEM_NOT_FOUND,
                                ProcurementErrorCode.ITEM_NOT_FOUND.getMessage() + ": " + cmd.itemSku());
                    }
                    Money unitPrice = Money.of(cmd.unitPrice());
                    return new PurchaseOrderLine(
                            null,
                            cmd.itemSku(),
                            info.itemName(),
                            info.unit(),
                            cmd.quantity(),
                            unitPrice
                    );
                })
                .toList();
    }
}
