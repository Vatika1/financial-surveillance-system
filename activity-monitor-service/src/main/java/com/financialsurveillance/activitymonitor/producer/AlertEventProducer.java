package com.financialsurveillance.activitymonitor.producer;

import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertStatus;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class AlertEventProducer {

    private final KafkaTemplate<String, AlertCreatedEvent> kafkaTemplate;
    @Value("${kafka.topics.alerts-created}")
    private String topic;

    private static final Logger log = LoggerFactory.getLogger(AlertEventProducer.class);

    public AlertEventProducer(KafkaTemplate<String, AlertCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAlert(RuleViolationDTO violation, TradeCreatedEvent event){

        String key = event.getAdvisorId();

        AlertCreatedEvent alertCreatedEvent = AlertCreatedEvent.builder()
                .alertId(UUID.randomUUID())
                .createdAt(event.getCreatedAt())
                .ruleId(violation.getRuleId())
                .advisorId(violation.getAdvisorId())
                .tradeId(violation.getTradeId())
                .severity(violation.getSeverity())
                .ruleName(violation.getRuleName())
                .violationDetails(violation.getViolationDetails())
                .status(AlertStatus.OPEN)
                .build();

        CompletableFuture<SendResult<String, AlertCreatedEvent>> future =
                kafkaTemplate.send(topic, key, alertCreatedEvent);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(
                        "Failed to publish AlertCreatedEvent. tradeEventId={}, tradeId={}, topic={}",
                        event.getId(),
                        event.getTradeId(),
                        topic,
                        throwable
                );
                return;
            }
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published AlertCreatedEvent successfully. tradeEventId={}, alertEventId={}, tradeId={}, topic={}, partition={}, offset={}",
                    event.getId(),
                    alertCreatedEvent.getAlertId(),
                    alertCreatedEvent.getTradeId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

    });


}
}

