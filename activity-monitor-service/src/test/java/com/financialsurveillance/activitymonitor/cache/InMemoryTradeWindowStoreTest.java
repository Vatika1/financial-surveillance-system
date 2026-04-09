package com.financialsurveillance.activitymonitor.cache;

import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class InMemoryTradeWindowStoreTest {

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
    void shouldReturnRecentlyAddedTrade_whenNewAdvisorTradeIsAdded(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event);
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001",  Duration.ofMinutes(10));
        assertEquals(1, recentTrades.size());
    }
    @Test
    void shouldReturnAllTrades_whenMultipleAdvisorTradesAreAdded(){
        TradeCreatedEvent event1 = getTradeCreatedEvent();
        TradeCreatedEvent event2 = TradeCreatedEvent.builder()
                .tradeId("TRD-002")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event1);
        store.addTrade("ADV-001", event2);
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofMinutes(10));
        assertEquals(2, recentTrades.size());
    }

    @Test
    void shouldNotReturnTradeForAdvisorId_whenCallingGetRecentTrades(){
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofMinutes(5));
        assertEquals(0, recentTrades.size());
    }
    @Test
    void shouldReturn1TradeForAdvisorId_whenCallingGetRecentTrades(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        event.setTradeTimestamp(ZonedDateTime.now().minusSeconds(30));
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event);
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofSeconds(60));
        assertEquals(1, recentTrades.size());
    }
    @Test
    void shouldNotReturnAnyTradeForAdvisorId_whenAddingTradeWithOldTimestamp(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        event.setTradeTimestamp(ZonedDateTime.now().minusMinutes(5));
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event);
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofSeconds(60));
        assertEquals(0, recentTrades.size());
    }
    @Test
    void shouldRemoveOldTrades_whenCleanupTrades(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        event.setTradeTimestamp(ZonedDateTime.now().minusMinutes(5));
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event);
        store.cleanUpTrades();
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofMinutes(10));
        assertEquals(0, recentTrades.size());
    }
    @Test
    void shouldKeepRecentTrades_whenCleanupTrades(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        event.setTradeTimestamp(ZonedDateTime.now().minusSeconds(30));
        InMemoryTradeWindowStore store = new InMemoryTradeWindowStore();
        store.addTrade("ADV-001", event);
        store.cleanUpTrades();
        List<TradeCreatedEvent> recentTrades = store.getRecentTrades("ADV-001", Duration.ofMinutes(10));
        assertEquals(1, recentTrades.size());
    }



}
