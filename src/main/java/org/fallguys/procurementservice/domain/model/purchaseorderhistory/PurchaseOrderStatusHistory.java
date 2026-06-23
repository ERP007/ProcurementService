package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.time.Instant;

/**
 * 발주서의 상태 변경 이력 한 건. (별도 테이블로 분리된 append-only 로그)
 *
 * poCode      : 대상 발주서 코드
 * status      : 전이된 상태
 * actorCode   : 상태를 변경한 담당자 코드
 * payload     : 상태별 부가 데이터(없으면 null)
 * createdAt   : 상태 변경 시각
 */
public record PurchaseOrderStatusHistory(
        String poCode,
        PurchaseOrderStatus status,
        String actorCode,
        StatusChangePayload payload,
        Instant createdAt
) {
    public static PurchaseOrderStatusHistory of(
            String poCode, PurchaseOrderStatus status, String actorCode,
            StatusChangePayload payload, Instant createdAt) {
        return new PurchaseOrderStatusHistory(poCode, status, actorCode, payload, createdAt);
    }

    // 부가 데이터 없는 전이(예: 자동 보상 롤백)용. payload는 null.
    public static PurchaseOrderStatusHistory of(
            String poCode, PurchaseOrderStatus status, String actorCode, Instant createdAt) {
        return new PurchaseOrderStatusHistory(poCode, status, actorCode, null, createdAt);
    }
}
