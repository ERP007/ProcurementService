package org.fallguys.procurementservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RabbitMQ 토폴로지.
 * - 발신: erp.commands(topic), routing key = 이벤트 wire string
 * - 수신: erp.event(topic) → procurement 소유 reply 큐로 입고 성공/실패 응답 바인딩
 */
@Configuration
@EnableScheduling
public class RabbitConfig {

    public static final String COMMANDS_EXCHANGE = "erp.commands";
    public static final String EVENT_EXCHANGE = "erp.events";
    public static final String REPLY_QUEUE = "procurement.inventory.reply";

    // 재시도 소진·처리 불가 메시지를 격리하는 데드레터
    public static final String REPLY_DLX = "procurement.inventory.reply.dlx";
    public static final String REPLY_DLQ = "procurement.inventory.reply.dlq";

    // inventory가 erp.event로 발행하는 입고 응답 routing key
    public static final String RK_INBOUND_APPLIED = "inventory.stock.inbound.applied.procurement";
    public static final String RK_INBOUND_REJECTED = "inventory.stock.inbound.rejected.procurement";

    @Bean
    TopicExchange commandsExchange() {
        return new TopicExchange(COMMANDS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    Queue replyQueue() {
        // 처리 실패(재시도 소진) 시 메시지를 DLX로 보낸다.
        return QueueBuilder.durable(REPLY_QUEUE)
                .deadLetterExchange(REPLY_DLX)
                .build();
    }

    @Bean
    FanoutExchange replyDlx() {
        return new FanoutExchange(REPLY_DLX, true, false);
    }

    @Bean
    Queue replyDlq() {
        return QueueBuilder.durable(REPLY_DLQ).build();
    }

    @Bean
    Binding bindReplyDlq(Queue replyDlq, FanoutExchange replyDlx) {
        return BindingBuilder.bind(replyDlq).to(replyDlx);
    }

    @Bean
    Binding bindInboundApplied(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_INBOUND_APPLIED);
    }

    @Bean
    Binding bindInboundRejected(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_INBOUND_REJECTED);
    }
}
