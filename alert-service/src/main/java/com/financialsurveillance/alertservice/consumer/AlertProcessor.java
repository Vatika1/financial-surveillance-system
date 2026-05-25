package com.financialsurveillance.alertservice.consumer;

import com.financialsurveillance.alertservice.service.AlertService;
import com.financialsurveillance.alertservice.service.IdempotencyService;
import com.financialsurveillance.events.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}
