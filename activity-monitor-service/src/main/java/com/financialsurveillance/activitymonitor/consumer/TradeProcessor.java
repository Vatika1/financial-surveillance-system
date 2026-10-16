package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.activitymonitor.service.IdempotencyService;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TradeProcessor {

    private final IdempotencyService idempotencyService;
    private final ActivityMonitorService activityMonitorService;

    @Transactional
    public void processBatchInTransaction(List<TradeCreatedEvent> events){
        List<TradeCreatedEvent> fresh = idempotencyService.filterUnprocessed(events);
        if (fresh.isEmpty()) {
            return;
        }
        idempotencyService.markProcessed(fresh.stream().map(TradeCreatedEvent::getTradeId).toList());
        activityMonitorService.processTrades(fresh);
    }
}