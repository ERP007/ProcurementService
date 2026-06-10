package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.CreateDraftPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.CreateDraftPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.outbound.*;
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
public class CreateDraftPurchaseOrderService implements CreateDraftPurchaseOrderUseCase {

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
     * 발주 초안을 생성한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 비즈니스 검증: 도착 희망일 1년 이내, 품목 코드 중복 없음.
     * 3) 공급사 조회: 비활성 포함 미존재 시 404.
     * 4) 창고 조회: 미존재 시 404.
     * 5) 품목 조회: 라인이 있는 경우 전체 itemSku를 한 번에 조회, 미존재 시 404.
     * 6) PO 코드 채번 후 도메인 객체 생성.
     * 7) 저장 후 반환.
     *
     * 트랜잭션: 쓰기. 채번·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 도착 희망일 1년 초과: BusinessException (400)
     * - 품목 코드 중복: BusinessException (400)
     * - 공급사·창고·품목 미존재: ResourceNotFoundException (404)
     */
    @Override
    @Transactional
    public PurchaseOrder createDraft(UserRole role, CreateDraftPurchaseOrderCommand command) {
        validateRole(role);
        validateDesiredArrivalDate(command.desiredArrivalDate());
        validateNoDuplicateItemSkus(command.lines());

        loadVendorPort.findActiveByCode(command.vendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND));

        loadWarehousePort.verifyActive(command.warehouseCode());

        List<PurchaseOrderLine> lines = resolveLines(command.lines());
        String code = generatePoCodePort.generate();

        PurchaseOrder purchaseOrder = new PurchaseOrder(
                code,
                command.vendorCode(),
                command.warehouseCode(),
                PurchaseOrderStatus.DRAFT,
                command.desiredArrivalDate(),
                command.memo(),
                lines,
                new ProcurementOrderCreation(command.userCode(), Instant.now()),
                null,
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

    private void validateNoDuplicateItemSkus(List<CreatePurchaseOrderLineCommand> lines) {
        if (lines == null || lines.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        boolean hasDuplicate = lines.stream()
                .map(CreatePurchaseOrderLineCommand::itemSku)
                .anyMatch(sku -> !seen.add(sku));
        if (hasDuplicate) {
            throw new BusinessValidationException(ProcurementErrorCode.DUPLICATE_ITEM_CODE);
        }
    }

    private List<PurchaseOrderLine> resolveLines(List<CreatePurchaseOrderLineCommand> lineCommands) {
        if (lineCommands == null || lineCommands.isEmpty()) return List.of();

        List<String> itemSkus = lineCommands.stream()
                .map(CreatePurchaseOrderLineCommand::itemSku)
                .toList();

        Map<String, ItemInfo> itemInfoMap = loadItemPort.loadAll(itemSkus);

        return lineCommands.stream()
                .map(cmd -> {
                    ItemInfo info = itemInfoMap.get(cmd.itemSku());
                    if (info == null) {
                        throw new ResourceNotFoundException(ProcurementErrorCode.ITEM_NOT_FOUND);
                    }
                    Money unitPrice = Money.of(cmd.unitPrice());
                    return new PurchaseOrderLine(
                            null,
                            info.itemSku(),
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
