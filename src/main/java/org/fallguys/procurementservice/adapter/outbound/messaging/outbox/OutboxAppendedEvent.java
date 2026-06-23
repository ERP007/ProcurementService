package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import java.util.UUID;

/**
 * outbox 행이 비즈니스 트랜잭션에 적재됐음을 알리는 내부 Spring 이벤트.
 * AFTER_COMMIT 리스너가 받아 즉시 발행한다(폴러는 보조).
 */
public record OutboxAppendedEvent(UUID eventId) {}
