package com.financialsurveillance.alertservice.producer;

import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertPersistedEventProducerTest {

    @Mock
    private KafkaTemplate<String, AlertPersistedEvent> kafkaTemplate;

    @InjectMocks
    private AlertPersistedEventProducer alertPersistedEventProducer;

    private AlertCreatedEvent getAlertCreatedEvent(){
        return AlertCreatedEvent.builder()
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertTypeId("AHS-20260327143022829")
                .tradeId("TRADE-001")
                .advisorId("ADVISOR-42")
                .ruleId("RULE_006")
                .ruleName("After Hours Trading")
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .createdAt(ZonedDateTime.of(2026, 3, 27, 14, 30, 22, 0, ZoneOffset.UTC))
                .violationDetails(null)
                .build();
    }

    private AlertDTO getAlertDTO(){
        return AlertDTO.builder()
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertTypeId("AHS-20260327143022829")
                .tradeId("TRADE-001")
                .advisorId("ADVISOR-42")
                .ruleId("RULE_006")
                .ruleName("After Hours Trading")
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .createdAt(ZonedDateTime.of(2026, 3, 27, 14, 30, 22, 0, ZoneOffset.UTC))
                .violationDetails(null)
                .build();
    }

    @Test
    void publishAlert_ShouldSuccessfullyPublish(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        AlertDTO dto = getAlertDTO();

        CompletableFuture<SendResult<String, AlertPersistedEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(future);

        alertPersistedEventProducer.publishAlert(dto, event);

        verify(kafkaTemplate).send(any(), eq(event.getAdvisorId()), any(AlertPersistedEvent.class));

    }
    @Test
    void ShouldLogError_WhenKafkaPublishFails(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        AlertDTO dto = getAlertDTO();
        CompletableFuture<SendResult<String, AlertPersistedEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(failedFuture);
        alertPersistedEventProducer.publishAlert(dto,event);
        verify(kafkaTemplate).send(any(), eq(event.getAdvisorId()), any(AlertPersistedEvent.class));
    }
}
