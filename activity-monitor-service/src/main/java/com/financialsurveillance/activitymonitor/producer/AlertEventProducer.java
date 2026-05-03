package com.financialsurveillance.activitymonitor.producer;

import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.exception.AlertPublishException;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertStatus;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AlertEventProducer {

    private final KafkaTemplate<String, AlertCreatedEvent> kafkaTemplate;
    @Value("${kafka.topics.alerts-created}")
    private String topic;

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);
    private static final Logger log = LoggerFactory.getLogger(AlertEventProducer.class);
    private static final Map<String, String> RULE_CODES = Map.ofEntries(
            Map.entry("RULE_001", "LTS"),
            Map.entry("RULE_002", "RRT"),
            Map.entry("RULE_003", "RS"),
            Map.entry("RULE_004", "PET"),
            Map.entry("RULE_005", "ET"),
            Map.entry("RULE_006", "AHS"),
            Map.entry("RULE_007", "FR"),
            Map.entry("RULE_008", "WT"),
            Map.entry("RULE_009", "LS"),
            Map.entry("RULE_010", "CR"),
            Map.entry("RULE_011", "CAT"),
            Map.entry("RULE_012", "LT")
    );

    public void publishAlert(RuleViolationDTO violation, TradeCreatedEvent event){

        String key = event.getAdvisorId();

        AlertCreatedEvent alertCreatedEvent = AlertCreatedEvent.builder()
                .alertId(UUID.randomUUID())
                .alertTypeId(generateAlertTypeId(violation.getRuleId()))
                .createdAt(event.getCreatedAt())
                .ruleId(violation.getRuleId())
                .advisorId(violation.getAdvisorId())
                .tradeId(violation.getTradeId())
                .severity(violation.getSeverity())
                .ruleName(violation.getRuleName())
                .violationDetails(violation.getViolationDetails())
                .status(AlertStatus.OPEN)
                .build();

        try {
            SendResult<String, AlertCreatedEvent> result = kafkaTemplate
                    .send(topic, key, alertCreatedEvent)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            log.info("Published AlertCreatedEvent. tradeId={} alertId={} topic={} partition={} offset={}",
                    alertCreatedEvent.getTradeId(),
                    alertCreatedEvent.getAlertId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (TimeoutException e) {
            log.error("Kafka send timed out tradeId={} alertId={} timeoutMs={}",
                    alertCreatedEvent.getTradeId(),
                    alertCreatedEvent.getAlertId(),
                    SEND_TIMEOUT.toMillis(), e);
            throw new AlertPublishException(
                    "Kafka publish timed out for alertId=" + alertCreatedEvent.getAlertId(), e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Kafka send failed tradeId={} alertId={} cause={}",
                    alertCreatedEvent.getTradeId(),
                    alertCreatedEvent.getAlertId(),
                    cause.getMessage(), cause);
            throw new AlertPublishException(
                    "Kafka publish failed for alertId=" + alertCreatedEvent.getAlertId(), cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlertPublishException(
                    "Interrupted while publishing alertId=" + alertCreatedEvent.getAlertId(), e);
        }
}
    private String generateAlertTypeId(String ruleId){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String code = RULE_CODES.get(ruleId);
        if (code == null) {
            throw new IllegalArgumentException("Unknown rule ID: " + ruleId);
        }
        return code + "-" + LocalDateTime.now().format(formatter);
    }


}

