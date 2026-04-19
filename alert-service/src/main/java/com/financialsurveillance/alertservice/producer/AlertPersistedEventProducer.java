package com.financialsurveillance.alertservice.producer;

import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AlertPersistedEventProducer {

    private final KafkaTemplate<String, AlertPersistedEvent> kafkaTemplate;
    @Value("${kafka.topics.alerts-persisted}")
    private String topic;

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

        CompletableFuture<SendResult<String, AlertPersistedEvent>> future =
                kafkaTemplate.send(topic, key, alertPersistedEvent);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(
                        "Failed to publish AlertPersistedEvent. alertId={}, alertTypeId={}, tradeId={}, advisorId={}, topic={}",
                        event.getAlertId(),
                        event.getAlertTypeId(),
                        event.getTradeId(),
                        event.getAdvisorId(),
                        topic,
                        throwable
                );
                return;
            }
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published AlertPersistedEvent successfully. alertId={}, alertTypeId={}, tradeId={}, advisorId={}, topic={}, partition={}, offset={}",
                    event.getAlertId(),
                    event.getAlertTypeId(),
                    event.getTradeId(),
                    event.getAdvisorId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

        });
    }

}
