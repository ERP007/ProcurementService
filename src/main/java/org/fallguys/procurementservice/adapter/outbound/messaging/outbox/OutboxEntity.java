package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.EventType;

import java.time.Instant;
import java.util.UUID;

/**
 * 트랜잭셔널 outbox 행. 비즈니스 트랜잭션과 같은 커밋으로 적재되고,
 * AFTER_COMMIT 발행 + @Scheduled 폴러가 PENDING을 집어 발행한다.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    private OutboxEntity(UUID eventId, String aggregateId, EventType eventType, String payload, Instant createdAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = createdAt;
    }

    // 신규 PENDING 행. eventId는 BaseEvent의 멱등 키를 그대로 PK로 사용.
    public static OutboxEntity pending(UUID eventId, String aggregateId, EventType eventType, String payload,
                                       Instant createdAt) {
        return new OutboxEntity(eventId, aggregateId, eventType, payload, createdAt);
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void increaseRetry() {
        this.retryCount++;
    }
}
