package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeEventConsumerTest {

    @Mock
    private TradeProcessor tradeProcessor;

    @InjectMocks
    private TradeEventConsumer tradeEventConsumer;

    private TradeCreatedEvent getTradeCreatedEvent(String tradeId){
        return TradeCreatedEvent.builder()
                .tradeId(tradeId)
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
    }
    @Test
    void ConsumeTrade_ShouldSuccessfullyConsume(){
        TradeCreatedEvent event = getTradeCreatedEvent("TRD-001");
        ConsumerRecord<String, TradeCreatedEvent> record =
                new ConsumerRecord<>("trades.raw", 0, 0L, "ADV-001", event);
        Acknowledgment ack = mock(Acknowledgment.class);

        tradeEventConsumer.consume(List.of(record), ack);

        verify(tradeProcessor).processBatchInTransaction(List.of(event));
        verify(ack).acknowledge();
    }

    @Test
    void ConsumeTrade_ShouldFail_whenNullTradeId(){
        TradeCreatedEvent event = getTradeCreatedEvent("TRD-001");
        event.setTradeId(null);
        ConsumerRecord<String, TradeCreatedEvent> record =
                new ConsumerRecord<>("trades.raw", 0, 0L, "ADV-001", event);
        Acknowledgment ack = mock(Acknowledgment.class);

        assertThrows(BatchListenerFailedException.class, () -> {
            tradeEventConsumer.consume(List.of(record), ack);
        });

        verify(tradeProcessor, never()).processBatchInTransaction(List.of(event));
        verify(ack, never()).acknowledge();
    }

    @Test
    void consume_ShouldNotAcknowledge_whenProcessorThrows(){
        TradeCreatedEvent event = getTradeCreatedEvent("TRD-001");
        Acknowledgment ack = mock(Acknowledgment.class);

        ConsumerRecord<String, TradeCreatedEvent> record =
                new ConsumerRecord<>("trades.raw", 0, 0L, "ADV-001", event);
        doThrow(new DataAccessResourceFailureException("DB failure"))
                .when(tradeProcessor).processBatchInTransaction(any());

        assertThrows(DataAccessResourceFailureException.class,
                () -> tradeEventConsumer.consume(List.of(record), ack));

        verify(tradeProcessor).processBatchInTransaction(List.of(event));
        verify(ack, never()).acknowledge();
    }
    @Test
    void consumeTrade_ShouldProcessAllTrades_whenBatchOfMultiple(){
        TradeCreatedEvent trade1 = getTradeCreatedEvent("TRD-001");
        TradeCreatedEvent trade2 = getTradeCreatedEvent("TRD-002");
        Acknowledgment ack = mock(Acknowledgment.class);

        ConsumerRecord<String, TradeCreatedEvent> record1 =
                new ConsumerRecord<>("trades.raw", 0, 0L, "ADV-001", trade1);
        ConsumerRecord<String, TradeCreatedEvent> record2 =
                new ConsumerRecord<>("trades.raw", 0, 1L, "ADV-001", trade2);

        tradeEventConsumer.consume(List.of(record1, record2), ack);

        verify(tradeProcessor).processBatchInTransaction(List.of(trade1, trade2));
        verify(ack).acknowledge();
    }

    @Test
    void consumeTrade_ShouldProcessValidPrefix_whenBatchContainsInvalidTrade(){
        TradeCreatedEvent trade1 = getTradeCreatedEvent("TRD-001");
        ConsumerRecord<String, TradeCreatedEvent> record1 =
                new ConsumerRecord<>("trades.raw", 0, 0L, "ADV-001", trade1);

        TradeCreatedEvent trade2 = getTradeCreatedEvent("TRD-002");
        trade2.setTradeId(null);
        ConsumerRecord<String, TradeCreatedEvent> record2 =
                new ConsumerRecord<>("trades.raw", 0, 1L, "ADV-001", trade2);

        Acknowledgment ack = mock(Acknowledgment.class);

        assertThrows(BatchListenerFailedException.class,
                () -> tradeEventConsumer.consume(List.of(record1, record2), ack));

        verify(tradeProcessor).processBatchInTransaction(List.of(trade1));
        verify(ack, never()).acknowledge();
    }
}
