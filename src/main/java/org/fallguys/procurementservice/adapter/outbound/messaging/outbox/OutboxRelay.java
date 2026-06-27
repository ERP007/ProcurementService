package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fallguys.procurementservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import org.fallguys.procurementservice.config.RabbitConfig;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 트랜잭셔널 outbox 릴레이. 두 경로로 발행한다.
 * 1) AFTER_COMMIT: 비즈니스 트랜잭션 커밋 직후 해당 행을 즉시 발행(주 경로).
 * 2) @Scheduled 폴러: AFTER_COMMIT 누락·발행 실패로 PENDING에 남은 행을 보조 발행.
 *
 * 발행 확정(publisher confirm ack) 시 outbox를 PUBLISHED로,
 * 발주 saga를 PROCESSING으로 전진시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int MAX_RETRY = 5;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final OutboxJpaDao outboxJpaDao;
    private final ConfirmStockEventPublishedUseCase confirmStockEventPublishedUseCase;

    @Async("outboxRelayExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppended(OutboxAppendedEvent event) {
        outboxJpaDao.findById(event.eventId())
                .filter(o -> o.getStatus() == OutboxStatus.PENDING)
                .ifPresent(this::publish);
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-delay-ms:5000}")
    public void publishPending() {
        List<OutboxEntity> pending = outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEntity entity : pending) {
            // 일시 장애(false)면 배치를 중단한다. MQ 다운 시 남은 행마다 confirm 타임아웃으로
            // 도는 폭주를 막고, 다음 폴 주기로 미뤄 자연 백오프시킨다.
            if (!publish(entity)) {
                break;
            }
        }
    }

    /**
     * 한 행을 발행하고 publisher confirm을 기다린다.
     * ack → outbox PUBLISHED + saga PROCESSING. nack/영구 실패 → retry 증가, 한계 초과 시 FAILED.
     * 브로커 일시 장애(transient) → 상태·retry를 건드리지 않고 PENDING 유지(폴러가 재발행).
     *
     * @return 일시 장애로 PENDING을 유지한 경우 false(폴러 배치 중단 신호), 그 외 true.
     */
    private boolean publish(OutboxEntity entity) {
        UUID eventId = entity.getEventId();
        Message message = MessageBuilder
                .withBody(entity.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(eventId.toString())
                .build();
        CorrelationData correlation = new CorrelationData(eventId.toString());

        try {
            rabbitTemplate.send(RabbitConfig.COMMANDS_EXCHANGE, entity.getEventType().getWire(),
                    message, correlation);
            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (confirm.ack()) {
                // saga 전진을 먼저 확정한다. outbox를 PUBLISHED로 바꾸기 전에 실패하면
                // 행이 PENDING으로 남아 폴러가 재발행→멱등 수렴한다(불일치가 self-healing).
                confirmStockEventPublishedUseCase.confirmPublished(entity.getAggregateId());
                entity.markPublished(Instant.now());
                outboxJpaDao.save(entity);
                return true;
            } else {
                // nack은 per-message 거부 신호라 transient로 보지 않는다.
                handleFailure(entity, "broker nack: " + confirm.reason());
                return true;
            }
        } catch (InterruptedException e) {
            // 풀 graceful shutdown을 위해 인터럽트 플래그를 복원하고 PENDING 유지.
            Thread.currentThread().interrupt();
            keepPending(entity, "interrupted while waiting for publisher confirm");
            return false;
        } catch (Exception e) {
            if (isTransient(e)) {
                keepPending(entity, e.getMessage());
                return false;
            }
            handleFailure(entity, e.getMessage());
            return true;
        }
    }

    /**
     * 브로커 일시 장애 판정(보수적 화이트리스트). 알려진 인프라 예외만 transient로 보고,
     * 그 외는 전부 영구/미상 실패로 취급해 retry→FAILED 안전밸을 태운다.
     * getFuture().get()이 던지는 ExecutionException은 getCause()로 언래핑해 판정한다.
     */
    private boolean isTransient(Throwable t) {
        Throwable cause = (t instanceof ExecutionException) ? t.getCause() : t;
        return cause instanceof AmqpConnectException
                || cause instanceof AmqpIOException
                || cause instanceof TimeoutException;
    }

    /**
     * 일시 장애 처리: 상태·retry를 변경하지 않고 PENDING으로 둔다(save 호출 없음).
     * MQ 복구 시 폴러가 그대로 재발행한다. WARN 1줄만 남긴다.
     */
    private void keepPending(OutboxEntity entity, String reason) {
        log.warn("outbox 발행 일시 장애 PENDING 유지 eventId={} reason={}", entity.getEventId(), reason);
    }

    private void handleFailure(OutboxEntity entity, String reason) {
        entity.increaseRetry();
        if (entity.getRetryCount() >= MAX_RETRY) {
            entity.markFailed();
            log.error("outbox 발행 실패 한계 초과 eventId={} reason={}", entity.getEventId(), reason);
        } else {
            log.warn("outbox 발행 실패 재시도 대기 eventId={} retry={} reason={}",
                    entity.getEventId(), entity.getRetryCount(), reason);
        }
        outboxJpaDao.save(entity);
    }
}
