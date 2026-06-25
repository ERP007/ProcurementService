package org.fallguys.procurementservice.adapter.outbound.messaging;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.BaseEvent;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.EventType;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.InboundStockPayload;
import org.fallguys.procurementservice.adapter.outbound.messaging.outbox.OutboxAppendedEvent;
import org.fallguys.procurementservice.adapter.outbound.messaging.outbox.OutboxEntity;
import org.fallguys.procurementservice.adapter.outbound.messaging.outbox.OutboxJpaDao;
import org.fallguys.procurementservice.application.port.outbound.model.Executor;
import org.fallguys.procurementservice.application.port.outbound.port.InboundStockPort;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * async 입고 어댑터: outbox 기반(기본). 동기 REST 방식은
 * {@link org.fallguys.procurementservice.adapter.outbound.client.StockInboundRestAdapter} 참고.
 * 어느 경로를 쓸지는 서비스가 {@code stock.sync-mode} 플래그로 분기한다.
 */
@Component
@RequiredArgsConstructor
public class StockInboundMessagingAdapter implements InboundStockPort {

    private final ObjectMapper objectMapper;
    private final OutboxJpaDao outboxJpaDao;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 입고 요청을 outbox에 적재한다(REST 동기 호출을 대체).
     *
     * 흐름:
     * 1) 도메인 PurchaseOrder + 실행자 → InboundStockPayload, BaseEvent envelope로 감싼다.
     * 2) envelope를 JSON 직렬화해 OutboxEntity(PENDING)로 INSERT한다.
     * 3) OutboxAppendedEvent를 발행한다(relay가 AFTER_COMMIT에 즉시 발행).
     *
     * 트랜잭션: 호출자(입고 유스케이스)의 쓰기 트랜잭션에 합류한다.
     * outbox INSERT가 비즈니스 상태 변경과 같은 커밋으로 묶여 원자성을 보장한다.
     * correlationId·aggregateId는 발주 code(saga 상관관계 키).
     */
    @Override
    public void inbound(PurchaseOrder order, Executor executor) {
        InboundStockPayload payload = InboundStockPayload.from(
                order,
                new InboundStockPayload.Executor(executor.code(), executor.name()));

        BaseEvent<InboundStockPayload> event =
                BaseEvent.of(EventType.INBOUND_STOCK_REQUESTED, order.getCode(), payload);

        String envelopeJson = objectMapper.writeValueAsString(event);

        outboxJpaDao.save(OutboxEntity.pending(
                event.eventId(),
                order.getCode(),
                EventType.INBOUND_STOCK_REQUESTED,
                envelopeJson,
                event.occurredAt()));

        eventPublisher.publishEvent(new OutboxAppendedEvent(event.eventId()));
    }
}
