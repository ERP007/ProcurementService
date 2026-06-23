package org.fallguys.procurementservice.adapter.inbound.event;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.inbound.event.dto.InventoryReplyEnvelope;
import org.fallguys.procurementservice.application.port.inbound.usecase.CompensateInboundUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.CompleteSagaUseCase;
import org.fallguys.procurementservice.config.RabbitConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * inventory 입고 처리 응답을 수신해 saga를 종결한다.
 * 라우팅 키로 성공/실패를 분기하며, 예외는 삼키지 않는다(재시도→DLQ 동작 보장).
 */
@Component
@RequiredArgsConstructor
public class InventoryReplyEventListener {

    // inventory가 멱등 중복으로 거절했으나 사실상 처리 완료를 뜻하는 에러 코드 → 성공 취급.
    private static final String ALREADY_PROCESSED = "ALREADY_PROCESSED";

    private final ObjectMapper objectMapper;
    private final CompleteSagaUseCase completeSagaUseCase;
    private final CompensateInboundUseCase compensateInboundUseCase;

    @RabbitListener(queues = RabbitConfig.REPLY_QUEUE)
    public void onReply(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        InventoryReplyEnvelope envelope = objectMapper.readValue(message.getBody(), InventoryReplyEnvelope.class);
        String poCode = envelope.correlationId();

        switch (routingKey) {
            case RabbitConfig.RK_INBOUND_APPLIED -> completeSagaUseCase.complete(poCode);
            case RabbitConfig.RK_INBOUND_REJECTED -> handleRejected(poCode, envelope);
            default -> throw new IllegalStateException("처리할 수 없는 reply routing key: " + routingKey);
        }
    }

    // 거절 응답. ALREADY_PROCESSED는 멱등 성공으로 간주, 그 외는 보상한다.
    private void handleRejected(String poCode, InventoryReplyEnvelope envelope) {
        String errorCode = envelope.payload() == null ? null : envelope.payload().errorCode();
        if (ALREADY_PROCESSED.equals(errorCode)) {
            completeSagaUseCase.complete(poCode);
            return;
        }
        String errorMessage = envelope.payload() == null ? null : envelope.payload().errorMessage();
        compensateInboundUseCase.compensate(poCode, errorCode, errorMessage);
    }
}
