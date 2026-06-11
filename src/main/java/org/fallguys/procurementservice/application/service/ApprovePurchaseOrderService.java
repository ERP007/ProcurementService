package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.ApprovePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovePurchaseOrderService implements ApprovePurchaseOrderUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER,
            UserRole.HQ_STAFF
    );

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadItemPort loadItemPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;

    /**
     * DRAFT 발주서를 승인한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 발주서 조회: 미존재 시 404.
     * 3) 품목 서비스 호출로 기존 라인의 SKU 존재 검증 및 스냅샷 갱신.
     * 4) 도메인 상태 머신 approve() 호출: DRAFT 가드 + 상태·approval·lines 변경.
     * 5) 저장 후 반환.
     *
     * 트랜잭션: 쓰기.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (404)
     * - 도착 희망일 1년 초과: BusinessValidationException (400)
     * - 존재하지 않는 SKU 포함: ResourceNotFoundException (404)
     * - DRAFT 아닌 상태: BusinessValidationException (400, 도메인이 던짐)
     */
    @Override
    @Transactional
    public PurchaseOrder approve(UserRole role, ApprovePurchaseOrderCommand command) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(ProcurementErrorCode.FORBIDDEN);
        }

        PurchaseOrder purchaseOrder = loadPurchaseOrderPort.findByCode(command.code())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND.getMessage() + ": " + command.code()
                ));

        validateDesiredArrivalDate(purchaseOrder.getDesiredArrivalDate());

        List<PurchaseOrderLine> validatedLines = buildValidatedLines(purchaseOrder.getLines());

        purchaseOrder.approve(
                new ProcurementOrderApproval(command.userCode(), Instant.now()),
                validatedLines
        );

        return savePurchaseOrderPort.save(purchaseOrder);
    }

    private void validateDesiredArrivalDate(LocalDate desiredArrivalDate) {
        if (desiredArrivalDate.isAfter(LocalDate.now().plusYears(1))) {
            throw new BusinessValidationException(ProcurementErrorCode.DESIRED_ARRIVAL_DATE_TOO_FAR);
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
