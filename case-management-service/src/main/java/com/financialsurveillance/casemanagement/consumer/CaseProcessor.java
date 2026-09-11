package com.financialsurveillance.casemanagement.consumer;

import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.casemanagement.service.IdempotencyService;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}
