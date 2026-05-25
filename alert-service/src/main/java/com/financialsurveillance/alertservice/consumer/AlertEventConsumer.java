package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.events.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertProcessor alertProcessor;

    @KafkaListener(
            topics = "${kafka.topics.alerts-created}",
            groupId = "alert-service",
            containerFactory = "alertKafkaListenerContainerFactory"
    )
    public void consume(AlertCreatedEvent event, Acknowledgment ack){
        log.info("Received alert: alertId={} alertTypeId={} tradeId={}, advisorId={}",
                    event.getAlertId(),event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

        if (event.getAlertId() == null) {
            throw new IllegalArgumentException("Invalid event: missing required fields");
        }

        try {
            alertProcessor.processInTransaction(event);
            log.info("Successfully processed alertId={}", event.getAlertId());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate alert detected, skipping: alertId={}", event.getAlertId());
        }
        ack.acknowledge();
    }
}
