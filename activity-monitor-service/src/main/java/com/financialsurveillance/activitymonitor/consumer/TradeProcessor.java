package com.financialsurveillance.activitymonitor.consumer;

import com.financialsurveillance.activitymonitor.service.ActivityMonitorService;
import com.financialsurveillance.activitymonitor.service.IdempotencyService;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TradeProcessor {

    private final IdempotencyService idempotencyService;
    private final ActivityMonitorService activityMonitorService;

    @Transactional
    public void processInTransaction(TradeCreatedEvent event) {
        idempotencyService.markProcessed(event.getTradeId());
        activityMonitorService.processTrade(event);
    }
}