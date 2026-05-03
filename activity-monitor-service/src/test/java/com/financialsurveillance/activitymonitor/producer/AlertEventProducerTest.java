package com.financialsurveillance.activitymonitor.producer;

import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.exception.AlertPublishException;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertEventProducerTest {

    @Mock
    private KafkaTemplate<String, AlertCreatedEvent> kafkaTemplate;

    @InjectMocks
    private AlertEventProducer alertEventProducer;

    private TradeCreatedEvent getAlertCreatedEvent(){
        return TradeCreatedEvent.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
    }

    private RuleViolationDTO getRuleViolationDTO(){
        return RuleViolationDTO.builder()
                .ruleId("RULE_001")
                .ruleName("Large Trade Size")
                .severity(AlertSeverity.HIGH)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .violationDescription("Trade value exceeds threshold")
                .violationDetails(null)
                .build();
    }
    @Test
    void publishAlert_ShouldSuccessfullyPublish(){
        TradeCreatedEvent event = getAlertCreatedEvent();

        RuleViolationDTO violation = getRuleViolationDTO();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("alerts.created", 0),  // topic, partition
                0L, 0, 0L, 0, 0                            // offset + others
        );
        ProducerRecord<String, AlertCreatedEvent> producerRecord =
                new ProducerRecord<>("alerts.created", event.getAdvisorId(), null);
        SendResult<String, AlertCreatedEvent> sendResult =
                new SendResult<>(producerRecord, metadata);

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        alertEventProducer.publishAlert(violation, event);
        verify(kafkaTemplate).send(any(), eq(event.getAdvisorId()), any(AlertCreatedEvent.class));

    }
    @Test
    void publishAlert_ShouldThrowAlertPublishException_WhenKafkaPublishFails() {
        TradeCreatedEvent event = getAlertCreatedEvent();
        RuleViolationDTO violation = getRuleViolationDTO();

        CompletableFuture<SendResult<String, AlertCreatedEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        AlertPublishException ex = assertThrows(
                AlertPublishException.class,
                () -> alertEventProducer.publishAlert(violation, event)
        );

        // optional: assert the message or cause to confirm we hit the right catch block
        assertThat(ex.getMessage()).contains("Kafka publish failed");

        verify(kafkaTemplate).send(any(), eq(event.getAdvisorId()), any(AlertCreatedEvent.class));
    }
    @Test
    void publishAlert_ShouldPublish_WithUnknownRuleId(){
        TradeCreatedEvent event = getAlertCreatedEvent();
        RuleViolationDTO violation = RuleViolationDTO.builder()
                .ruleId("RULE_999")
                .ruleName("Large Trade Size")
                .severity(AlertSeverity.HIGH)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .violationDescription("Trade value exceeds threshold")
                .violationDetails(null)
                .build();
        assertThrows(IllegalArgumentException.class, () -> {
            alertEventProducer.publishAlert(violation, event);
        });

        verify(kafkaTemplate, never()).send(any(), any(), any());

    }
}
