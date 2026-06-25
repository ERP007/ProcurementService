package org.fallguys.procurementservice.adapter.outbound.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 발행 이벤트 공통 envelope. payload(T)를 감싸 메타데이터를 싣는다.
 *
 * - eventId: 멱등 키(소비측 중복 제거).
 * - eventVersion: 스키마 버전(현재 1 고정).
 * - producer: 발행 서비스 식별자.
 * - correlationId: 분산 추적/saga 상관관계 키.
 */
public record BaseEvent<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        Instant occurredAt,
        String correlationId,
        T payload
) {
    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "procurement-service";

    public static <T> BaseEvent<T> of(EventType eventType, String correlationId, T payload) {
        return new BaseEvent<>(
                UUID.randomUUID(),
                eventType.getWire(),
                EVENT_VERSION,
                PRODUCER,
                Instant.now(),
                correlationId,
                payload
        );
    }

    // 발생 시각을 명시 지정한다(payload.occurredAt과 envelope.occurredAt을 일치시킬 때).
    public static <T> BaseEvent<T> of(EventType eventType, String correlationId, Instant occurredAt, T payload) {
        return new BaseEvent<>(
                UUID.randomUUID(),
                eventType.getWire(),
                EVENT_VERSION,
                PRODUCER,
                occurredAt,
                correlationId,
                payload
        );
    }
}
