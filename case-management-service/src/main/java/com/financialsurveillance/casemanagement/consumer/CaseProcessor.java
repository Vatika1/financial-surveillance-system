package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.casemanagement.service.IdempotencyService;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CaseProcessor {

    private final IdempotencyService idempotencyService;
    private final CaseService caseService;

    @Transactional
    public void processInTransaction(AlertPersistedEvent event) {
        idempotencyService.markProcessed(event.getAlertId());
        caseService.createCaseFromAlert(event);
    }

    @Transactional
    public void processBatchInTransaction(List<AlertPersistedEvent> events) {
        List<AlertPersistedEvent> fresh = idempotencyService.filterUnprocessed(events);
        if (fresh.isEmpty()) {
            return;
        }
        idempotencyService.markProcessed(fresh.stream().map(AlertPersistedEvent::getAlertId).toList());
        caseService.createCasesFromAlerts(fresh);
    }
}