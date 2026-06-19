package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderhistory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "purchase_order_status_history",
        indexes = @Index(name = "idx_po_status_history_po_code", columnList = "po_code"))
@Getter
@NoArgsConstructor
public class PurchaseOrderStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // payload는 어댑터가 직렬화한 JSON 문자열을 받는다(부가 데이터 없으면 null).
    public static PurchaseOrderStatusHistoryEntity from(PurchaseOrderStatusHistory domain, String payloadJson) {
        PurchaseOrderStatusHistoryEntity entity = new PurchaseOrderStatusHistoryEntity();
        entity.poCode = domain.poCode();
        entity.status = domain.status();
        entity.actorCode = domain.actorCode();
        entity.payload = payloadJson;
        entity.createdAt = domain.createdAt();
        return entity;
    }

    public PurchaseOrderStatusHistory toDomain(StatusChangePayload payload) {
        return new PurchaseOrderStatusHistory(poCode, status, actorCode, payload, createdAt);
    }
}
