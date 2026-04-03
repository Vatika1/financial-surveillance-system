package com.financialsurveillance.activitymonitor.producer;

import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
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

        CompletableFuture<SendResult<String, AlertCreatedEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(future);
        alertEventProducer.publishAlert(violation, event);
        verify(kafkaTemplate).send(any(), eq(event.getAdvisorId()), any(AlertCreatedEvent.class));

    }
    @Test
     void publishAlert_ShouldLogError_WhenKafkaPublishFails(){
        TradeCreatedEvent event = getAlertCreatedEvent();

        RuleViolationDTO violation = getRuleViolationDTO();
        CompletableFuture<SendResult<String, AlertCreatedEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(failedFuture);

        alertEventProducer.publishAlert(violation, event);

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
