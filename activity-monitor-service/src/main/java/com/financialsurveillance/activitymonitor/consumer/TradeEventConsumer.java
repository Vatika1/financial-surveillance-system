package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

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
    public void consume(List<ConsumerRecord<String, TradeCreatedEvent>> records, Acknowledgment ack){
        List<TradeCreatedEvent> events = records.stream().map(ConsumerRecord::value).toList();
        List<String> correlationIds = records.stream()
                .map(r -> r.headers().lastHeader("correlationId"))
                .filter(Objects::nonNull)
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .distinct()
                .toList();
        log.info("Received batch of {} trades correlationIds={}", events.size(), correlationIds);
        if (!correlationIds.isEmpty()) {
            MDC.put("correlationId", correlationIds.get(0));
        }

        try{
            int firstInvalid = -1;
            for (int i = 0; i < events.size(); i++) {
                TradeCreatedEvent event = events.get(i);
                if (event == null || event.getTradeId() == null) {
                    firstInvalid = i;
                    break;
                }
            }
            List<TradeCreatedEvent> valid = firstInvalid < 0 ? events : events.subList(0, firstInvalid);
            if (!valid.isEmpty()) {
                tradeProcessor.processBatchInTransaction(valid);
                log.info("Successfully processed {} trades", valid.size());
            }
            if (firstInvalid >= 0) {
                throw new BatchListenerFailedException(
                        "Invalid event: missing required fields",
                        new IllegalArgumentException("Invalid event: missing required fields"),
                        firstInvalid
                );
            }
            ack.acknowledge();
        }
        finally {
            MDC.remove("correlationId");
        }
    }

}