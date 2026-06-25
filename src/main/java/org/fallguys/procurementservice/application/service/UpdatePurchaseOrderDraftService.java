package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.inbound.command.UpdatePurchaseOrderDraftCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.UpdatePurchaseOrderDraftUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;
import org.fallguys.procurementservice.domain.model.purchaseorder.WarehouseRef;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdatePurchaseOrderDraftService implements UpdatePurchaseOrderDraftUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadVendorPort loadVendorPort;
    private final LoadWarehousePort loadWarehousePort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final PublishUserActivityPort publishUserActivityPort;

    /**
     * DRAFT 상태 발주서를 수정한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 발주서 조회: 미존재 시 404.
     * 3) 상태 검증: DRAFT가 아니면 수정 불가.
     * 4) 비즈니스 검증: 품목 코드 중복 없음.
     * 5) 공급사 조회: 미존재 시 404.
     * 6) 창고 조회: 미존재·비활성 시 404/400.
     * 7) 라인 재구성(스냅샷 없이).
     * 8) 기존 creation 정보를 유지한 채 도메인 객체 재생성 후 저장.
     *
     * 트랜잭션: 쓰기.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (404)
     * - DRAFT 아닌 상태: BusinessValidationException (400)
     * - 품목 코드 중복: BusinessValidationException (400)
     * - 공급사·창고 미존재: ResourceNotFoundException (404)
     */
    @Override
    @Transactional
    public PurchaseOrder update(UserRole role, UpdatePurchaseOrderDraftCommand command) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder existing = loadPurchaseOrderPort.findByCode(command.code())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND.getMessage() + ": " + command.code()
                ));

        if (existing.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }

        validateNoDuplicateItemSkus(command.lines());

        loadVendorPort.findActiveByCode(command.vendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND));

        loadWarehousePort.verifyActive(command.warehouseCode());

        List<PurchaseOrderLine> lines = buildDraftLines(command.lines());

        // DRAFT는 미확정 → 공급사·창고명 박제 X. code만 보관하고 조회 시 live로 채운다.
        existing.updateDraft(
                VendorRef.codeOnly(command.vendorCode()),
                WarehouseRef.codeOnly(command.warehouseCode()),
                command.memo(),
                lines
        );

        PurchaseOrder saved = savePurchaseOrderPort.save(existing);

        // 사용자 활동: 수정은 DRAFT만 → 공급사명 미박제이므로 content는 null. 상태 불변이라 status도 null.
        publishUserActivityPort.publish(new UserActivity(
                command.userCode(), UserActivityType.UPDATED,
                saved.getCode(), null, Instant.now(), null));

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
}
