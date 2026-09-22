package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {
    private final TradeProcessor tradeProcessor;

    @KafkaListener(
            topics = "${kafka.topics.trades-raw}",
            groupId = "activity-monitor-service",
            containerFactory = "tradeKafkaListenerContainerFactory"
    )
    public void consume(TradeCreatedEvent event, @Header(value = "correlationId", required = false) byte[] correlationId, Acknowledgment ack) {
        if (correlationId != null) {
            MDC.put("correlationId", new String(correlationId, StandardCharsets.UTF_8));
        }

        try {
            log.info("Received trade: tradeId={}, advisorId={}", event.getTradeId(), event.getAdvisorId());
            if (event.getTradeId() == null) {
                throw new IllegalArgumentException("Invalid event: missing required fields");
            }

            try {
                tradeProcessor.processInTransaction(event);
                log.info("Successfully processed tradeId={}", event.getTradeId());
            } catch (DataIntegrityViolationException ex) {
                log.warn("Duplicate trade detected, skipping: tradeId={}", event.getTradeId());
            }
            ack.acknowledge();
        } finally {
            MDC.remove("correlationId");
        }

    }
}