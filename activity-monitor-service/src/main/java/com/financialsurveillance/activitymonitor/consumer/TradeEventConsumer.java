package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.exception.TradeProcessingException;
import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {
    private final ActivityMonitorService activityMonitorService;
    @KafkaListener(
            topics = "${kafka.topics.trades-raw}",
            groupId = "activity-monitor-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TradeCreatedEvent event, Acknowledgment ack){

        try {
            log.info("Received trade: tradeId={}, advisorId={}", event.getTradeId(), event.getAdvisorId());

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
            activityMonitorService.processTrade(event);

            // ✅ Mark processed
            //idempotencyService.markProcessed(event.getOrderId());

            // ✅ Manual acknowledgment (only after success)
            ack.acknowledge();

            log.info("Successfully processed tradeId={}", event.getTradeId());

        } catch (Exception ex) {
            // ❗ Do NOT acknowledge → message will be retried
            // Depending on config: retry / DLQ
            throw new TradeProcessingException(event.getTradeId(), event.getAdvisorId(), ex);
        }
    }
}
