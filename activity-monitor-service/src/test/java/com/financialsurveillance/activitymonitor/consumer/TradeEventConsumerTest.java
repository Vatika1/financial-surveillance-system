package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeEventConsumerTest {
    @Mock
    private ActivityMonitorService activityMonitorService;

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
        verify(activityMonitorService).processTrade(event);
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeTrade_ShouldFail_whenNullTradeId(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        event.setTradeId(null);
        assertThrows(IllegalArgumentException.class, () -> {
            tradeEventConsumer.consume(event, mock(Acknowledgment.class));
        });
    }
    @Test
    void ConsumeTrade_ShouldFail_TradeProcessingExceptionWhenServiceFailure(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        doThrow(new RuntimeException("DB down")).when(activityMonitorService).processTrade(any());
        Acknowledgment ack = mock(Acknowledgment.class);
        assertThrows(RuntimeException.class, () -> {
            tradeEventConsumer.consume(event, ack);
        });
        verify(activityMonitorService).processTrade(event);
        verify(mock(Acknowledgment.class), never()).acknowledge();
    }
}
