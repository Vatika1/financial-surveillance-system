package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPersistedEventConsumer {

    private final CaseService caseService;

    @KafkaListener(
            topics = "${kafka.topics.alerts-persisted}",
            groupId = "case-management-service",
            containerFactory = "caseKafkaListenerContainerFactory"
    )
    public void consume(AlertPersistedEvent event, Acknowledgment ack){
        try {
            log.info("Received alert: alertId={} alertTypeId={} tradeId={}, advisorId={}",
                    event.getAlertId(),event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

            // ✅ Validation
            if (event == null || event.getAlertId() == null || event.getAdvisorId() == null) {
                log.warn("Received invalid AlertPersistedEvent, skipping. event={}", event);
                ack.acknowledge(); // ack it so it doesn't redeliver garbage forever
                return;
            }

            // ✅ Business logic
            caseService.createCaseFromAlert(event);

            // ✅ Mark processed
            //idempotencyService.markProcessed(event.getOrderId());

            // ✅ Manual acknowledgment (only after success)
            ack.acknowledge();

            log.info("Successfully processed alertId={}", event.getAlertId());

        } catch (Exception ex) {
            // ❗ Do NOT acknowledge → message will be retried
            // Depending on config: retry / DLQ
            log.error("Failed to process AlertPersistedEvent. alertId={}, advisorId={}",
                    event.getAlertId(), event.getAdvisorId(), ex);
        }

    }

}
