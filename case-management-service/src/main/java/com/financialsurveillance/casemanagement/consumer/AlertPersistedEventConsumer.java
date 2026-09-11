package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPersistedEventConsumer {

    private final CaseProcessor caseProcessor;

    @KafkaListener(
            topics = "${kafka.topics.alerts-persisted}",
            groupId = "case-management-service",
            containerFactory = "caseKafkaListenerContainerFactory"
    )
    public void consume(AlertPersistedEvent event, Acknowledgment ack){

            log.info("Received alert: alertId={} alertTypeId={} tradeId={}, advisorId={}",
                    event.getAlertId(),event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

            // ✅ Validation
            if (event.getAlertId() == null) {
                throw new IllegalArgumentException("Invalid event: missing required fields");
            }

            try {
                caseProcessor.processInTransaction(event);
                log.info("Successfully processed alertId={}", event.getAlertId());
            } catch (DataIntegrityViolationException ex) {
                log.warn("Duplicate alert detected, skipping: alertId={}", event.getAlertId());
            }
            ack.acknowledge();
    }
}
