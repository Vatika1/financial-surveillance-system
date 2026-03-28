package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.exception.AlertProcessingException;
import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.events.AlertCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertEventConsumerTest {

    @Mock
    private AlertService alertService;

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
        verify(alertService).processAlert(event);
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeAlert_ShouldFail_whenNullTradeId(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        event.setTradeId(null);

        assertThrows(AlertProcessingException.class, () -> {
            alertEventConsumer.consume(event, mock(Acknowledgment.class));
        });
    }

    @Test
    void ConsumeAlert_ShouldFail_AlertProcessingExceptionWhenServiceFailure(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        doThrow(new RuntimeException("DB down")).when(alertService).processAlert(any());

        Acknowledgment ack = mock(Acknowledgment.class);

        assertThrows(AlertProcessingException.class, () -> {
            alertEventConsumer.consume(event, ack);
        });
        verify(alertService).processAlert(event);
        verify(mock(Acknowledgment.class), never()).acknowledge();
    }
}
