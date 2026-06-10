package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.GeneratePoCodePort;
import org.fallguys.procurementservice.application.port.outbound.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.*;
import org.fallguys.procurementservice.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreatePurchaseOrderService implements CreatePurchaseOrderUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER,
            UserRole.HQ_STAFF
    );

    private final LoadVendorPort loadVendorPort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadItemPort loadItemPort;
    private final GeneratePoCodePort generatePoCodePort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;

    /**
     * 발주를 생성한다. (DRAFT: 임시 저장 / APPROVED: 즉시 승인)
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 비즈니스 검증: 도착 희망일 1년 이내, 품목 코드 중복 없음.
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
     * - 도착 희망일 1년 초과: BusinessValidationException (400)
     * - 품목 코드 중복: BusinessValidationException (400)
     * - 공급사·창고 미존재: ResourceNotFoundException (404)
     * - 존재하지 않는 SKU 포함(APPROVED): ResourceNotFoundException (404)
     */
    @Override
    @Transactional
    public PurchaseOrder create(UserRole role, CreatePurchaseOrderCommand command) {
        validateRole(role);
        validateDesiredArrivalDate(command.desiredArrivalDate());
        validateNoDuplicateItemSkus(command.lines());

        loadVendorPort.findActiveByCode(command.vendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND));

        loadWarehousePort.verifyActive(command.warehouseCode());

        String code = generatePoCodePort.generate();
        Instant now = Instant.now();
        boolean isApproved = command.status() == PurchaseOrderStatus.APPROVED;
        List<PurchaseOrderLine> lines = isApproved
                ? buildApprovedLines(command.lines())
                : buildDraftLines(command.lines());

        PurchaseOrder purchaseOrder = new PurchaseOrder(
                code,
                command.vendorCode(),
                command.warehouseCode(),
                command.status(),
                command.desiredArrivalDate(),
                command.memo(),
                lines,
                new ProcurementOrderCreation(command.userCode(), now),
                isApproved ? new ProcurementOrderApproval(command.userCode(), now) : null,
                null,
                null
        );

        return savePurchaseOrderPort.save(purchaseOrder);
    }

    private void validateRole(UserRole role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(ProcurementErrorCode.FORBIDDEN);
        }
    }

    private void validateDesiredArrivalDate(LocalDate desiredArrivalDate) {
        if (desiredArrivalDate.isAfter(LocalDate.now().plusYears(1))) {
            throw new BusinessValidationException(ProcurementErrorCode.DESIRED_ARRIVAL_DATE_TOO_FAR);
        }
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
                            unitPrice,
                            unitPrice.multiply(cmd.quantity())
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
                            unitPrice,
                            unitPrice.multiply(cmd.quantity())
                    );
                })
                .toList();
    }
}
