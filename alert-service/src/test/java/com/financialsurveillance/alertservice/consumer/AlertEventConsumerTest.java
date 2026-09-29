package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.exception.AlertProcessingException;
import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.events.AlertCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertEventConsumerTest {

    @Mock
    private AlertProcessor alertProcessor;

    @InjectMocks
    private AlertEventConsumer alertEventConsumer;

    private AlertCreatedEvent getAlertCreatedEvent(UUID alertId){
        return AlertCreatedEvent.builder()
                .alertId(alertId)
                .alertTypeId("AHS-20260327143022829")
                .tradeId("TRADE-001")
                .advisorId("ADVISOR-42")
                .ruleId("RULE_006")
                .ruleName("After Hours Trading")
                .build();
    }

    @Test
    void ConsumeAlert_ShouldSuccessfullyConsume(){
        AlertCreatedEvent event = getAlertCreatedEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        ConsumerRecord<String, AlertCreatedEvent> record =
                new ConsumerRecord<>("alerts.created", 0, 0L, event.getAdvisorId(), event);
        Acknowledgment ack = mock(Acknowledgment.class);

        alertEventConsumer.consume(List.of(record),  ack);
        verify(alertProcessor).processBatchInTransaction(List.of(event));
        verify(ack).acknowledge();
    }

    @Test
    void consumeAlert_ShouldProcessAllEvents_whenBatchOfMultiple(){
        AlertCreatedEvent event1 = getAlertCreatedEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        AlertCreatedEvent event2 = getAlertCreatedEvent(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        ConsumerRecord<String, AlertCreatedEvent> record1 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event1.getAdvisorId(), event1);

        ConsumerRecord<String, AlertCreatedEvent> record2 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event2.getAdvisorId(), event2);
        Acknowledgment ack = mock(Acknowledgment.class);

        alertEventConsumer.consume(List.of(record1, record2),  ack);
        verify(alertProcessor).processBatchInTransaction(List.of(event1, event2));
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeAlert_ShouldFail_whenNullAlertId(){
        AlertCreatedEvent event = getAlertCreatedEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        event.setAlertId(null);
        ConsumerRecord<String, AlertCreatedEvent> record =
                new ConsumerRecord<>("alerts.created", 0, 0L, event.getAdvisorId(), event);
        Acknowledgment ack = mock(Acknowledgment.class);

        assertThrows(BatchListenerFailedException.class, () -> {
            alertEventConsumer.consume(List.of(record), ack);
        });

        verify(alertProcessor, never()).processBatchInTransaction(any());
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumeAlert_ShouldProcessValidPrefix_whenBatchContainsInvalidEvent(){
        AlertCreatedEvent event1 = getAlertCreatedEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        ConsumerRecord<String, AlertCreatedEvent> record1 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event1.getAdvisorId(), event1);

        AlertCreatedEvent event2 = getAlertCreatedEvent(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        event2.setAlertId(null);
        ConsumerRecord<String, AlertCreatedEvent> record2 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event2.getAdvisorId(), event2);
        Acknowledgment ack = mock(Acknowledgment.class);

        assertThrows(BatchListenerFailedException.class, () -> {
            alertEventConsumer.consume(List.of(record1, record2), ack);
        });
        verify(alertProcessor).processBatchInTransaction(any());
        verify(ack, never()).acknowledge();
    }

    @Test
    void consumeAlert_ShouldNotAcknowledge_whenProcessorThrows(){
        AlertCreatedEvent event1 = getAlertCreatedEvent(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        AlertCreatedEvent event2 = getAlertCreatedEvent(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        ConsumerRecord<String, AlertCreatedEvent> record1 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event1.getAdvisorId(), event1);

        ConsumerRecord<String, AlertCreatedEvent> record2 =
                new ConsumerRecord<>("alerts.created", 0, 0L, event2.getAdvisorId(), event2);
        Acknowledgment ack = mock(Acknowledgment.class);

        doThrow(new RuntimeException("DB down"))
                .when(alertProcessor).processBatchInTransaction(any());

        assertThrows(RuntimeException.class, () -> {
            alertEventConsumer.consume(List.of(record1, record2), ack);
        });

        verify(alertProcessor).processBatchInTransaction(any());
        verify(ack, never()).acknowledge();
    }
}
