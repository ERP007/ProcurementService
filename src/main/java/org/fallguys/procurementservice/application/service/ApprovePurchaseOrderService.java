package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.ApprovePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.port.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;
import org.fallguys.procurementservice.domain.model.purchaseorder.WarehouseRef;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovePurchaseOrderService implements ApprovePurchaseOrderUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadItemPort loadItemPort;
    private final LoadVendorPort loadVendorPort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadWarehouseInfoPort loadWarehouseInfoPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    private final PublishUserActivityPort publishUserActivityPort;

    /**
     * DRAFT 발주서를 승인한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 발주서 조회: 미존재 시 404.
     * 2-1) 상태 가드: DRAFT가 아니면 불필요한 외부 호출 전에 fail-fast (도메인 approve()가 최종 방어).
     * 3) 비즈니스 검증: 라인 1개 이상.
     * 4) 공급사 조회: 미존재·비활성 시 404. (DRAFT 저장 후 벤더가 비활성화됐을 수 있다.)
     * 5) 창고 조회: 미존재·비활성 시 404/400. (DRAFT 저장 후 창고가 비활성화됐을 수 있다.)
     * 6) 품목 서비스 호출로 기존 라인의 SKU 존재 검증 및 스냅샷 갱신.
     * 7) 도메인 상태 머신 approve() 호출: DRAFT 가드 + 상태·approval·lines 변경.
     * 8) 저장 후 반환.
     *
     * 트랜잭션: 쓰기. 외부 호출(품목·창고 서비스)은 트랜잭션 경계 밖이며 실패 시 전체 롤백.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (404)
     * - 라인 없음: BusinessValidationException (400)
     * - 공급사·창고 미존재: ResourceNotFoundException (404)
     * - 비활성 창고: BusinessValidationException (400)
     * - 존재하지 않는 SKU 포함: ResourceNotFoundException (404)
     * - DRAFT 아닌 상태: BusinessValidationException (400)
     */
    @Override
    @Transactional
    public PurchaseOrder approve(UserRole role, ApprovePurchaseOrderCommand command) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder purchaseOrder = loadPurchaseOrderPort.findByCode(command.code())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND.getMessage() + ": " + command.code()
                ));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }

        validateHasLines(purchaseOrder.getLines());

        Vendor vendor = loadVendorPort.findActiveByCode(purchaseOrder.getVendor().code())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND));

        loadWarehousePort.verifyActive(purchaseOrder.getWarehouse().code());
        WarehouseInfo warehouseInfo = loadWarehouseInfoPort.findByCode(purchaseOrder.getWarehouse().code());

        List<PurchaseOrderLine> validatedLines = buildValidatedLines(purchaseOrder.getLines());

        // 확정 시점: 신뢰 출처(공급사·창고 master)에서 조회한 이름을 박제한다.
        purchaseOrder.approve(
                VendorRef.snapshot(vendor.getCode(), vendor.getName()),
                WarehouseRef.snapshot(warehouseInfo.code(), warehouseInfo.name()),
                validatedLines);

        PurchaseOrder saved = savePurchaseOrderPort.save(purchaseOrder);

        Instant now = Instant.now();
        savePurchaseOrderStatusHistoryPort.append(new PurchaseOrderStatusHistory(
                saved.getCode(),
                PurchaseOrderStatus.APPROVED,
                new ActorRef(command.userCode(), command.userName(), command.userPosition()),
                null,
                now
        ));

        // 사용자 활동: 승인은 확정 시점 → 공급사명 박제값을 content로, status는 출고대기(APPROVED).
        publishUserActivityPort.publish(new UserActivity(
                command.userCode(), UserActivityType.APPROVED,
                saved.getCode(), vendor.getName(), now,
                UserActivity.statusLabel(PurchaseOrderStatus.APPROVED)));

        return saved;
    }

    private void validateHasLines(List<PurchaseOrderLine> lines) {
        if (lines.isEmpty()) {
            throw new BusinessValidationException(ProcurementErrorCode.EMPTY_PURCHASE_ORDER_LINE);
        }
    }

    private List<PurchaseOrderLine> buildValidatedLines(List<PurchaseOrderLine> lines) {
        if (lines.isEmpty()) return List.of();

        List<String> skus = lines.stream()
                .map(PurchaseOrderLine::getItemSku)
                .toList();

        Map<String, ItemInfo> itemInfoMap = loadItemPort.loadAll(skus);

        return lines.stream()
                .map(line -> {
                    ItemInfo info = itemInfoMap.get(line.getItemSku());
                    if (info == null) {
                        throw new ResourceNotFoundException(ProcurementErrorCode.ITEM_NOT_FOUND,
                                ProcurementErrorCode.ITEM_NOT_FOUND.getMessage() + ": " + line.getItemSku());
                    }
                    return new PurchaseOrderLine(
                            line.getId(),
                            line.getItemSku(),
                            info.itemName(),
                            info.unit(),
                            line.getOrderQuantity(),
                            line.getUnitPrice()
                    );
                })
                .toList();
    }
}
