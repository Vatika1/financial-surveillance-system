package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.exception.AlertProcessingException;
import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.events.AlertCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertEventConsumerTest {

    @Mock
    private AlertProcessor alertProcessor;

    @InjectMocks
    private AlertEventConsumer alertEventConsumer;

    private AlertCreatedEvent getAlertCreatedEvent(){
        return AlertCreatedEvent.builder()
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertTypeId("AHS-20260327143022829")
                .tradeId("TRADE-001")
                .advisorId("ADVISOR-42")
                .ruleId("RULE_006")
                .ruleName("After Hours Trading")
                .build();
    }

    @Test
    void ConsumeAlert_ShouldSuccessfullyConsume(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);

        alertEventConsumer.consume(event, ack);
        verify(alertProcessor).processInTransaction(event);
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeAlert_ShouldFail_whenNullAlertId(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);
        event.setAlertId(null);

        assertThrows(IllegalArgumentException.class, () -> {
            alertEventConsumer.consume(event, ack);
        });

        verify(alertProcessor, never()).processInTransaction(event);
        verify(ack, never()).acknowledge();
    }

    @Test
    void ConsumeTrade_ShouldFail_DataIntegrityViolationExceptionWhenDuplicateAlert(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);

        doThrow(new DataIntegrityViolationException("Duplicate alert: " + event.getAlertId()))
                .when(alertProcessor).processInTransaction(any());

        alertEventConsumer.consume(event, ack);
        verify(alertProcessor).processInTransaction(event);
        verify(ack).acknowledge();
    }
}
