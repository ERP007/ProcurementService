package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.domain.model.purchaseorder.ProgressOutcome;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderProgress;

/**
 * 진행 페이지 폴링 응답 모델. pending·outcome은 백엔드가 saga 상태로 판단(조합 규칙 미노출).
 * failureReason은 outcome이 FAILED일 때만 채운다.
 */
public record PurchaseOrderProgressView(
        String code,
        PurchaseOrderProgress progress,
        boolean pending,
        ProgressOutcome outcome,
        String failureReason
) {
    public static PurchaseOrderProgressView from(PurchaseOrder order) {
        ProgressOutcome outcome = order.sagaOutcome();
        String reason = outcome == ProgressOutcome.FAILED ? order.getLastFailureReason() : null;
        return new PurchaseOrderProgressView(
                order.getCode(),
                order.progress(),
                outcome == ProgressOutcome.PENDING,
                outcome,
                reason
        );
    }
}
