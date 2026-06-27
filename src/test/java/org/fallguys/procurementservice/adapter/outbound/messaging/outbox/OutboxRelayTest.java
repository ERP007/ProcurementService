package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import org.fallguys.procurementservice.adapter.outbound.messaging.event.EventType;
import org.fallguys.procurementservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final int MAX_RETRY = 5;

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OutboxJpaDao outboxJpaDao;
    @Mock private ConfirmStockEventPublishedUseCase confirmStockEventPublishedUseCase;

    @InjectMocks
    private OutboxRelay relay;

    // ── ack ────────────────────────────────────────────────────────────────

    @Test
    void ack이면_PUBLISHED로_전환되고_saga확정과_save가_호출된다() {
        OutboxEntity entity = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubConfirm(true, null);

        relay.publishPending();

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isNotNull();
        verify(confirmStockEventPublishedUseCase).confirmPublished(entity.getAggregateId());
        verify(outboxJpaDao).save(entity);
    }

    // ── 일시 장애(transient) → PENDING 유지, DB 쓰기 없음 ──────────────────────

    @Test
    void 연결실패면_PENDING_유지되고_retry불변에_save미호출이다() {
        OutboxEntity entity = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        willThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .given(rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));

        relay.publishPending();

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isZero();
        verify(outboxJpaDao, never()).save(any());
        verifyNoInteractions(confirmStockEventPublishedUseCase);
    }

    @Test
    void confirm타임아웃이면_PENDING_유지되고_retry불변에_save미호출이다() {
        OutboxEntity entity = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        // future를 TimeoutException으로 완결 → get()이 ExecutionException(cause=TimeoutException)을 던진다.
        willAnswer(inv -> {
            CorrelationData correlation = inv.getArgument(3);
            correlation.getFuture().completeExceptionally(new TimeoutException("confirm timed out"));
            return null;
        }).given(rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));

        relay.publishPending();

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isZero();
        verify(outboxJpaDao, never()).save(any());
        verifyNoInteractions(confirmStockEventPublishedUseCase);
    }

    // ── nack → retry, 한계에서 FAILED ─────────────────────────────────────────

    @Test
    void nack이면_retry가_증가한다() {
        OutboxEntity entity = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubConfirm(false, "rejected");

        relay.publishPending();

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        verify(outboxJpaDao).save(entity);
        verifyNoInteractions(confirmStockEventPublishedUseCase);
    }

    @Test
    void nack이_한계직전에서_발생하면_FAILED로_전환된다() {
        OutboxEntity entity = pendingWithRetry(MAX_RETRY - 1);
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubConfirm(false, "rejected");

        relay.publishPending();

        assertThat(entity.getRetryCount()).isEqualTo(MAX_RETRY);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxJpaDao).save(entity);
    }

    // ── 영구/미상 예외 → handleFailure(retry++) ───────────────────────────────

    @Test
    void 영구또는미상_예외면_retry가_증가하고_PENDING_유지된다() {
        OutboxEntity entity = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        willThrow(new AmqpException("non-transient amqp failure"))
                .given(rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));

        relay.publishPending();

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        verify(outboxJpaDao).save(entity);
    }

    // ── 폴러 배치 단락 ─────────────────────────────────────────────────────────

    @Test
    void 일시장애면_폴러_배치가_단락되어_다음행은_시도되지_않는다() {
        OutboxEntity first = pending();
        OutboxEntity second = pending();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(first, second));
        willThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .given(rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));

        relay.publishPending();

        verify(rabbitTemplate, times(1))
                .send(any(), any(), any(Message.class), any(CorrelationData.class));
        verify(outboxJpaDao, never()).save(any());
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────────

    private void stubConfirm(boolean ack, String reason) {
        willAnswer(inv -> {
            CorrelationData correlation = inv.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).given(rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));
    }

    private OutboxEntity pending() {
        return OutboxEntity.pending(UUID.randomUUID(), "PO-2026-05-0001",
                EventType.INBOUND_STOCK_REQUESTED, "{\"payload\":true}", Instant.parse("2026-05-01T09:00:00Z"));
    }

    private OutboxEntity pendingWithRetry(int retryCount) {
        OutboxEntity entity = pending();
        for (int i = 0; i < retryCount; i++) {
            entity.increaseRetry();
        }
        return entity;
    }
}
