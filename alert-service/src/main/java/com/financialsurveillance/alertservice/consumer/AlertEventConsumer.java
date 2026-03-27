package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.exception.AlertProcessingException;
import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertService alertService;

    @KafkaListener(
            topics = "${kafka.topics.alerts-created}",
            groupId = "alert-service",
            containerFactory = "alertKafkaListenerContainerFactory"
    )
    public void consume(AlertCreatedEvent event, Acknowledgment ack){
        try {
            log.info("Received alert: alertId={} alertTypeId={} tradeId={}, advisorId={}",
                    event.getAlertId(),event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

            // ✅ Validation
            if (event.getTradeId() == null) {
                throw new IllegalArgumentException("Invalid event: missing required fields");
            }

            // ✅ Idempotency check (very important in real systems)
//            if (idempotencyService.isProcessed(event.getOrderId())) {
//                log.warn("Duplicate event received for orderId={}", event.getOrderId());
//                ack.acknowledge();
//                return;
//            }

            // ✅ Business logic
            alertService.processAlert(event);

            // ✅ Mark processed
            //idempotencyService.markProcessed(event.getOrderId());

            // ✅ Manual acknowledgment (only after success)
            ack.acknowledge();

            log.info("Successfully processed orderId={}", event.getTradeId());

        } catch (Exception ex) {
            // ❗ Do NOT acknowledge → message will be retried
            // Depending on config: retry / DLQ
            throw new AlertProcessingException(event.getAlertId(), event.getAlertTypeId(), event.getAdvisorId(), ex);
        }

    }
}
