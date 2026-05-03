package com.financialsurveillance.activitymonitor.cache;

import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryTradeWindowStore implements TradeWindowStore{

    private final ConcurrentHashMap<String, List<TradeCreatedEvent>> recentTrades = new ConcurrentHashMap<>();

    private static final Duration WINDOW = Duration.ofSeconds(60);

    @Override
    public void addTrade(String advisorId, TradeCreatedEvent trade) {
        recentTrades
                .computeIfAbsent(advisorId, k -> new CopyOnWriteArrayList<>())
                .add(trade);
    }

    @Override
    public void removeTrade(String advisorId, TradeCreatedEvent trade) {
        List<TradeCreatedEvent> trades = recentTrades.get(advisorId);
        if (trades != null) {
            trades.remove(trade);
            if (trades.isEmpty()) {
                recentTrades.remove(advisorId);
            }
        }
    }

    @Override
    public List<TradeCreatedEvent> getRecentTrades(String advisorId, Duration window) {
        List<TradeCreatedEvent> trades = recentTrades.get(advisorId);
        if (trades == null) {
            return Collections.emptyList();
        }
        ZonedDateTime  cutoff = ZonedDateTime.now().minus(window);
        return trades.stream()
                .filter(t -> t.getTradeTimestamp().isAfter(cutoff))
                .toList();
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void cleanUpTrades() {

        ZonedDateTime cutoff = ZonedDateTime.now().minus(WINDOW);
        for (Map.Entry<String, List<TradeCreatedEvent>> entry : recentTrades.entrySet()) {
            String advisorId = entry.getKey();
            List<TradeCreatedEvent> trades = entry.getValue();
            trades.removeIf(t -> t.getTradeTimestamp().isBefore(cutoff));
            if(trades.isEmpty()){
                recentTrades.remove(advisorId);
            }
        }
    }
}
