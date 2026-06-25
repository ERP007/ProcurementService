package org.fallguys.procurementservice.adapter.outbound.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.BaseEvent;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.EventType;
import org.fallguys.procurementservice.adapter.outbound.messaging.event.UserActivityPayload;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.config.RabbitConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 사용자 활동 이벤트 발행 어댑터. erp.events(topic) / user.activity.occurred로 직접 발행.
 *
 * 발행 시점: 호출자 트랜잭션이 있으면 AFTER_COMMIT(롤백 시 미발행), 없으면 즉시(fallbackExecution).
 * 보장 수준: best-effort. publisher confirm을 기다리지 않으며 발행 실패는 로그만 남기고 삼킨다
 * (활동 이력 유실은 허용, 발주 본 흐름을 막지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityMessagingAdapter implements PublishUserActivityPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // 발행 envelope를 트랜잭션 동기화 리스너로 넘기는 내부 이벤트.
    public record UserActivityAppendedEvent(BaseEvent<UserActivityPayload> envelope) {}

    @Override
    public void publish(UserActivity activity) {
        BaseEvent<UserActivityPayload> event = BaseEvent.of(
                EventType.USER_ACTIVITY_OCCURRED,
                activity.title(),
                activity.occurredAt(),
                new UserActivityPayload(
                        activity.employeeNo() == null ? UserActivityPayload.SYSTEM : activity.employeeNo(),
                        activity.type().action(),
                        activity.occurredAt(),
                        activity.title(),
                        activity.content(),
                        activity.status()));

        eventPublisher.publishEvent(new UserActivityAppendedEvent(event));
    }

    // 호출자 트랜잭션 커밋 후 발행한다(롤백 시 미발행). 트랜잭션이 없으면 fallbackExecution으로 즉시 발행.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAppended(UserActivityAppendedEvent event) {
        BaseEvent<UserActivityPayload> envelope = event.envelope();
        try {
            // 컨버터 빈이 없어 기본 SimpleMessageConverter(Java 직렬화)를 타지 않도록 직접 JSON 직렬화한다.
            byte[] body = objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8);
            Message message = MessageBuilder.withBody(body)
                    .setMessageId(envelope.eventId().toString())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.send(
                    RabbitConfig.EVENT_EXCHANGE,
                    EventType.USER_ACTIVITY_OCCURRED.getWire(),
                    message);
        } catch (Exception e) {
            // 유실 허용: 활동 이력 발행 실패는 본 흐름을 막지 않는다.
            log.warn("사용자 활동 이벤트 발행 실패 action={} title={} reason={}",
                    envelope.payload().action(), envelope.payload().title(), e.getMessage());
        }
    }
}
