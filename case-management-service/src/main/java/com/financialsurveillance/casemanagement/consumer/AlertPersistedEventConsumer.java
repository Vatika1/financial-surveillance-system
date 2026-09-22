package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
public class AlertPersistedEventConsumer {

    private final CaseProcessor caseProcessor;

    @KafkaListener(
            topics = "${kafka.topics.alerts-persisted}",
            groupId = "case-management-service",
            containerFactory = "caseKafkaListenerContainerFactory"
    )
    public void consume(List<ConsumerRecord<String, AlertPersistedEvent>> records, Acknowledgment ack) {
        List<AlertPersistedEvent> events = records.stream().map(ConsumerRecord::value).toList();
        List<String> correlationIds = records.stream()
                .map(r -> r.headers().lastHeader("correlationId"))
                .filter(Objects::nonNull)
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .distinct()
                .toList();
        log.info("Received batch of {} alerts correlationIds={}", events.size(), correlationIds);

        // Find the first invalid record. Everything before it is processed normally;
        // the invalid one goes to the DLT; everything after it is redelivered on the next poll.
        int firstInvalid = -1;
        for (int i = 0; i < events.size(); i++) {
            AlertPersistedEvent event = events.get(i);
            if (event == null || event.getAlertId() == null) {
                firstInvalid = i;
                break;
            }
        }

        List<AlertPersistedEvent> valid = firstInvalid < 0 ? events : events.subList(0, firstInvalid);

        if (!valid.isEmpty()) {
            caseProcessor.processBatchInTransaction(valid);
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
}