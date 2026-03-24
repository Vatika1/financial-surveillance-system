package com.financialsurveillance.activitymonitor.cache;

import com.financialsurveillance.events.TradeCreatedEvent;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface TradeWindowStore {

    void addTrade(String advisorId, TradeCreatedEvent trade);

    List<TradeCreatedEvent> getRecentTrades(String advisorId,  Duration window);

    void cleanUpTrades();
}
