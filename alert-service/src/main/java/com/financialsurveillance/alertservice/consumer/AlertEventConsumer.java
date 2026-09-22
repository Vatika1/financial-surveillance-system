package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.events.AlertCreatedEvent;
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
public class AlertEventConsumer {

    private final AlertProcessor alertProcessor;

    @KafkaListener(
            topics = "${kafka.topics.alerts-created}",
            groupId = "alert-service",
            containerFactory = "alertKafkaListenerContainerFactory"
    )
    public void consume(AlertCreatedEvent event, @Header(value = "correlationId", required = false) byte[] correlationId, Acknowledgment ack){

        if (correlationId != null) {
            MDC.put("correlationId", new String(correlationId, StandardCharsets.UTF_8));
        }
        try {
            // existing body unchanged
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
        } finally {
            MDC.remove("correlationId");
        }
        log.info("Received alert: alertId={} alertTypeId={} tradeId={}, advisorId={}",
                    event.getAlertId(),event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

    }
}
