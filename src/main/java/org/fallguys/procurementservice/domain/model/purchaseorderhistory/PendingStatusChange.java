package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.time.Instant;

/**
 * saga가 걸린 상태 전환의 in-flight 데이터. (saga 확정 전까지의 staging)
 *
 * 행위 시점(approve/receive 등)에만 아는 부가 데이터(payload + actor 스냅샷)를
 * saga 응답(reply)이 올 때까지 보관한다. reply는 보통 correlationId(=poCode)만 가져오므로
 * 그 사이의 데이터를 애그리거트나 read-model에 욱여넣지 않고 전용 staging에 둔다.
 *
 * poCode     : 대상 발주서 코드(애그리거트당 동시 진행 saga는 1건이므로 사실상 PK)
 * status     : 확정 시 기록될 전이 상태
 * actor      : 행위를 일으킨 행위자(행위 시점 박제: code·name·position)
 * payload    : 상태별 부가 데이터(없으면 null)
 * occurredAt : 행위 시점. 이력 승격 시 history.createdAt으로 그대로 보존해 KPI 타임스탬프를 유지한다.
 */
public record PendingStatusChange(
        String poCode,
        PurchaseOrderStatus status,
        ActorRef actor,
        StatusChangePayload payload,
        Instant occurredAt
) {
    // 확정된 milestone으로 승격. 행위 시점(occurredAt)을 이력 시각으로 보존한다.
    public PurchaseOrderStatusHistory toHistory() {
        return new PurchaseOrderStatusHistory(poCode, status, actor, payload, occurredAt);
    }
}
