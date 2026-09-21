package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public void consume(List<AlertPersistedEvent> events, Acknowledgment ack) {

        log.info("Received batch of {} alerts", events.size());

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