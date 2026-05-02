package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeEventConsumerTest {
    @Mock
    private ActivityMonitorService activityMonitorService;

    @Mock
    private TradeProcessor tradeProcessor;

    @InjectMocks
    private TradeEventConsumer tradeEventConsumer;

    private TradeCreatedEvent getTradeCreatedEvent(){
        return TradeCreatedEvent.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
    }
    @Test
    void ConsumeTrade_ShouldSuccessfullyConsume(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);
        tradeEventConsumer.consume(event, ack);
        verify(tradeProcessor).processInTransaction(event);
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeTrade_ShouldFail_whenNullTradeId(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);
        event.setTradeId(null);
        assertThrows(IllegalArgumentException.class, () -> {
            tradeEventConsumer.consume(event, ack);
        });

        verify(tradeProcessor, never()).processInTransaction(event);
        verify(ack, never()).acknowledge();
    }

    @Test
    void ConsumeTrade_ShouldFail_DataIntegrityViolationExceptionWhenDuplicateTrade(){
        TradeCreatedEvent event1 = getTradeCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);

        doThrow(new DataIntegrityViolationException("Duplicate trade: " + event1.getTradeId()))
                .when(tradeProcessor).processInTransaction(any());

            tradeEventConsumer.consume(event1, ack);

        verify(tradeProcessor).processInTransaction(event1);
        verify(ack).acknowledge();
    }
    @Test
    void ConsumeTrade_ShouldFail_TradeProcessingExceptionWhenServiceFailure(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        Acknowledgment ack = mock(Acknowledgment.class);

        doThrow(new RuntimeException("DB down"))
                .when(tradeProcessor).processInTransaction(any());

        assertThrows(RuntimeException.class, () -> {
            tradeEventConsumer.consume(event, ack);
        });
        verify(tradeProcessor).processInTransaction(event);
        verify(ack, never()).acknowledge();
    }
}
