package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.activitymonitor.service.IdempotencyService;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

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
    public void consume(TradeCreatedEvent event, Acknowledgment ack) {
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
    }
}