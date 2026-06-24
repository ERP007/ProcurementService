package org.fallguys.procurementservice.adapter.outbound.persistence.pendingstatuschange;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PendingStatusChange;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * saga in-flight 상태 전환 staging. po_code가 PK(애그리거트당 동시 진행 saga 1건).
 * 확정(DONE) 시 이력으로 승격 후 삭제, 실패(FAILED) 시 이력 없이 삭제된다.
 */
@Entity
@Table(name = "pending_status_change")
@Getter
@NoArgsConstructor
public class PendingStatusChangeEntity {

    @Id
    @Column(name = "po_code", nullable = false)
    private String poCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseOrderStatus status;

    @Column(name = "actor_code", nullable = false)
    private String actorCode;

    // 상태별 부가 데이터 JSON. 직렬화/역직렬화는 어댑터(ObjectMapper)가 담당.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // payload는 어댑터가 직렬화한 JSON 문자열을 받는다(부가 데이터 없으면 null).
    public static PendingStatusChangeEntity from(PendingStatusChange domain, String payloadJson) {
        PendingStatusChangeEntity entity = new PendingStatusChangeEntity();
        entity.poCode = domain.poCode();
        entity.status = domain.status();
        entity.actorCode = domain.actorCode();
        entity.payload = payloadJson;
        entity.occurredAt = domain.occurredAt();
        return entity;
    }

    public PendingStatusChange toDomain(StatusChangePayload payload) {
        return new PendingStatusChange(poCode, status, actorCode, payload, occurredAt);
    }
}
