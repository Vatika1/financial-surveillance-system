package com.financialsurveillance.activitymonitor.producer;

import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.exception.AlertPublishException;
import com.financialsurveillance.events.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertEventProducer, "topic", "alerts.created");
    }

    private TradeCreatedEvent getAlertCreatedEvent(String tradeId){
        return TradeCreatedEvent.builder()
                .tradeId(tradeId)
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

    private AlertCreatedEvent alertEvent(UUID alertId) {
        return AlertCreatedEvent.builder()
                .alertId(alertId)
                .alertTypeId("RS-20261015120000000")
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .ruleId("RULE_003")
                .ruleName("Restricted Security")
                .createdAt(ZonedDateTime.now())
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .build();
    }

    @Test
    void publishAll_ShouldSendAllAlerts(){
        AlertCreatedEvent alert1 = alertEvent(UUID.randomUUID());
        AlertCreatedEvent alert2 = alertEvent(UUID.randomUUID());

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        alertEventProducer.publishAll(List.of(alert1, alert2));

        ArgumentCaptor<ProducerRecord<String, AlertCreatedEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);

        verify(kafkaTemplate, times(2)).send(captor.capture());
        List<ProducerRecord<String, AlertCreatedEvent>> records = captor.getAllValues();

        assertEquals("alerts.created", records.get(0).topic());
        assertEquals(alert1.getAdvisorId(), records.get(0).key());
        assertEquals(alert1, records.get(0).value());

        assertEquals("alerts.created", records.get(1).topic());
        assertEquals(alert2.getAdvisorId(), records.get(1).key());
        assertEquals(alert2, records.get(1).value());
    }
//    @Test
//    void publishAlert_ShouldThrowAlertPublishException_WhenKafkaPublishFails() {
//        TradeCreatedEvent event = getAlertCreatedEvent();
//        RuleViolationDTO violation = getRuleViolationDTO();
//
//        CompletableFuture<SendResult<String, AlertCreatedEvent>> failedFuture =
//                CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
//        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failedFuture);
//
//        AlertPublishException ex = assertThrows(
//                AlertPublishException.class,
//                () -> alertEventProducer.publishAlert(violation, event)
//        );
//
//        // optional: assert the message or cause to confirm we hit the right catch block
//        assertThat(ex.getMessage()).contains("Kafka publish failed");
//
//        verify(kafkaTemplate).send(any(ProducerRecord.class));
//    }
//    @Test
//    void publishAlert_ShouldPublish_WithUnknownRuleId(){
//        TradeCreatedEvent event = getAlertCreatedEvent();
//        RuleViolationDTO violation = RuleViolationDTO.builder()
//                .ruleId("RULE_999")
//                .ruleName("Large Trade Size")
//                .severity(AlertSeverity.HIGH)
//                .advisorId("ADV-001")
//                .tradeId("TRD-001")
//                .violationDescription("Trade value exceeds threshold")
//                .violationDetails(null)
//                .build();
//        assertThrows(IllegalArgumentException.class, () -> {
//            alertEventProducer.publishAlert(violation, event);
//        });
//
//        verify(kafkaTemplate, never()).send(any(), any(), any());
//
//    }
}
