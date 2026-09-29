package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.alertservice.service.IdempotencyService;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AlertProcessor {

    private final IdempotencyService idempotencyService;
    private final AlertService alertService;

    @Transactional
    public void processInTransaction(AlertCreatedEvent event) {
        idempotencyService.markProcessed(event.getAlertId());
        alertService.processAlert(event);
    }

    @Transactional
    public void processBatchInTransaction(List<AlertCreatedEvent> events){
        List<AlertCreatedEvent> fresh = idempotencyService.filterUnprocessed(events);
        if (fresh.isEmpty()) {
            return;
        }
        idempotencyService.markProcessed(fresh.stream().map(AlertCreatedEvent::getAlertId).toList());
        alertService.processAlerts(fresh);
    }
}
