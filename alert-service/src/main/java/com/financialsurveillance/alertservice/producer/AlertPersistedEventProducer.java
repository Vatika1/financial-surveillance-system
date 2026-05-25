package com.financialsurveillance.alertservice.producer;

import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.alertservice.exception.AlertPersistedPublishException;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
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
    public void publishAlert(AlertDTO dto, AlertCreatedEvent event){
        String key = event.getAdvisorId();

        AlertPersistedEvent alertPersistedEvent = AlertPersistedEvent.builder()
                .alertId(dto.getAlertId())
                .alertTypeId(dto.getAlertTypeId())
                .persistedAt(ZonedDateTime.now())
                .advisorId(dto.getAdvisorId())
                .createdAt(dto.getCreatedAt())
                .ruleId(dto.getRuleId())
                .tradeId(dto.getTradeId())
                .status(dto.getStatus())
                .severity(dto.getSeverity())
                .ruleName(dto.getRuleName())
                .violationDetails(dto.getViolationDetails())
                .build();

        try {
            SendResult<String, AlertPersistedEvent> result = kafkaTemplate
                    .send(topic, key, alertPersistedEvent)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            log.info("Published AlertPersistedEvent. alertId={} alertTypeId={} tradeId={} topic={} partition={} offset={}",
                    alertPersistedEvent.getTradeId(),
                    alertPersistedEvent.getAlertId(),
                    alertPersistedEvent.getAlertTypeId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (TimeoutException e) {
            log.error("Kafka send timed out tradeId={} alertId={} timeoutMs={}",
                    alertPersistedEvent.getTradeId(),
                    alertPersistedEvent.getAlertId(),
                    SEND_TIMEOUT.toMillis(), e);
            throw new AlertPersistedPublishException(
                    "Kafka publish timed out for alertId=" + alertPersistedEvent.getAlertId(), e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Kafka send failed tradeId={} alertId={} cause={}",
                    alertPersistedEvent.getTradeId(),
                    alertPersistedEvent.getAlertId(),
                    cause.getMessage(), cause);
            throw new AlertPersistedPublishException(
                    "Kafka publish failed for alertId=" + alertPersistedEvent.getAlertId(), cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlertPersistedPublishException(
                    "Interrupted while publishing alertId=" + alertPersistedEvent.getAlertId(), e);
        }
    }

}
