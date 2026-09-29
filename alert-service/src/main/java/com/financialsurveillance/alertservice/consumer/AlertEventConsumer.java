package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.events.AlertCreatedEvent;
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
public class AlertEventConsumer {

    private final AlertProcessor alertProcessor;

    @KafkaListener(
            topics = "${kafka.topics.alerts-created}",
            groupId = "alert-service",
            containerFactory = "alertKafkaListenerContainerFactory"
    )
    public void consume(List<ConsumerRecord<String, AlertCreatedEvent>> records, Acknowledgment ack){
        List<AlertCreatedEvent> events = records.stream().map(ConsumerRecord::value).toList();
        List<String> correlationIds = records.stream()
                .map(r -> r.headers().lastHeader("correlationId"))
                .filter(Objects::nonNull)
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .distinct()
                .toList();
        log.info("Received batch of {} alerts correlationIds={}", events.size(), correlationIds);

        if (!correlationIds.isEmpty()) {
            MDC.put("correlationId", correlationIds.get(0));
        }

        try{
            int firstInvalid = -1;
            for (int i = 0; i < events.size(); i++) {
                AlertCreatedEvent event = events.get(i);
                if (event == null || event.getAlertId() == null) {
                    firstInvalid = i;
                    break;
                }
            }
            List<AlertCreatedEvent> valid = firstInvalid < 0 ? events : events.subList(0, firstInvalid);

            if (!valid.isEmpty()) {
                alertProcessor.processBatchInTransaction(valid);
                log.info("Successfully processed {} alerts", valid.size());
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
        finally{
            MDC.remove("correlationId");
        }

    }
}
