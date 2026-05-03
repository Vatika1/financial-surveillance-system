package com.financialsurveillance.activitymonitor.cache;

import com.financialsurveillance.events.TradeCreatedEvent;

import java.time.Duration;
import java.util.List;

public interface TradeWindowStore {

    void addTrade(String advisorId, TradeCreatedEvent trade);

    void removeTrade(String advisorId, TradeCreatedEvent trade);

    List<TradeCreatedEvent> getRecentTrades(String advisorId,  Duration window);

    void cleanUpTrades();
}
