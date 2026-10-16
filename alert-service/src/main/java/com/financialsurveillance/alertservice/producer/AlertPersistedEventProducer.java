package com.financialsurveillance.alertservice.producer;

import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.alertservice.exception.AlertPersistedPublishException;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class AlertPersistedEventProducer {

    private final KafkaTemplate<String, AlertPersistedEvent> kafkaTemplate;
    @Value("${kafka.topics.alerts-persisted}")
    private String topic;

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(AlertPersistedEventProducer.class);

    public void publishAll(List<AlertPersistedEvent> events){
        String correlationId = MDC.get("correlationId");

        List<CompletableFuture<SendResult<String, AlertPersistedEvent>>> futures =
                new ArrayList<>(events.size());

        for (AlertPersistedEvent event : events){
            ProducerRecord<String, AlertPersistedEvent> record =
                    new ProducerRecord<>(topic, event.getAdvisorId(), event);
            if (correlationId != null) {
                record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
            }
            futures.add(kafkaTemplate.send(record));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            log.info("Published {} AlertPersistedEvents", events.size());
        } catch (TimeoutException | ExecutionException e) {
            throw new AlertPersistedPublishException("Batch publish failed, size=" + events.size(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlertPersistedPublishException("Interrupted during batch publish", e);
        }
    }

}
