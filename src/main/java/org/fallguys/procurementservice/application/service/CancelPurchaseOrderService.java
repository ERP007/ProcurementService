package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.CancelPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.CancelPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.CancellationPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CancelPurchaseOrderService implements CancelPurchaseOrderUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER
    );

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    private final PublishUserActivityPort publishUserActivityPort;

    /**
     * 발주서를 취소한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 도메인 cancel() 호출: DRAFT·APPROVED 가드 + 상태·cancellation 변경.
     * 4) 저장 후 반환한다.
     *
     * 트랜잭션: 쓰기. 조회·취소·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     * - DRAFT·APPROVED 외 상태: BusinessValidationException (PO-03-08, 400, 도메인이 던짐)
     */
    @Override
    @Transactional
    public PurchaseOrder cancel(UserRole role, CancelPurchaseOrderCommand command) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder order = loadPurchaseOrderPort.findByCode(command.code())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        order.cancel();

        PurchaseOrder saved = savePurchaseOrderPort.save(order);

        Instant now = Instant.now();
        savePurchaseOrderStatusHistoryPort.append(new PurchaseOrderStatusHistory(
                saved.getCode(),
                PurchaseOrderStatus.CANCELED,
                new ActorRef(command.userCode(), command.userName(), command.userPosition()),
                new CancellationPayload(command.reason()),
                now
        ));

        // 사용자 활동: 공급사명 박제값(APPROVED 거친 경우)이 있으면 content로, DRAFT 취소면 null. status는 취소.
        publishUserActivityPort.publish(new UserActivity(
                command.userCode(), UserActivityType.CANCELED,
                saved.getCode(), saved.getVendor().nameSnapshot(), now,
                UserActivity.statusLabel(PurchaseOrderStatus.CANCELED)));

        return saved;
    }
}
