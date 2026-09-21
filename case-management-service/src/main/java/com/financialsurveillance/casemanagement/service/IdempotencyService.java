package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.casemanagement.domain.ProcessedAlert;
import com.financialsurveillance.casemanagement.repository.ProcessedAlertRepository;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyService {

    private final ProcessedAlertRepository processedAlertRepository;

    public void markProcessed(UUID alertId){
        ProcessedAlert processedAlert = ProcessedAlert.builder()
                .alertId(alertId)
                .build();
        processedAlertRepository.saveAndFlush(processedAlert);
        log.debug("Marked processed: alertId={}", alertId);
    }

    /**
     * Drops alerts already recorded in processed_alerts, and duplicates within the batch itself.
     * One query for the whole batch. Order is preserved.
     */
    public List<AlertPersistedEvent> filterUnprocessed(List<AlertPersistedEvent> events) {
        Map<UUID, AlertPersistedEvent> byAlertId = new LinkedHashMap<>();
        for (AlertPersistedEvent event : events) {
            byAlertId.putIfAbsent(event.getAlertId(), event);
        }

        Set<UUID> alreadyProcessed = processedAlertRepository.findAllById(byAlertId.keySet()).stream()
                .map(ProcessedAlert::getAlertId)
                .collect(Collectors.toSet());

        int skipped = events.size() - byAlertId.size() + alreadyProcessed.size();
        if (skipped > 0) {
            log.warn("Duplicate alert detected: skipping {} of {} in batch", skipped, events.size());
        }

        return byAlertId.values().stream()
                .filter(event -> !alreadyProcessed.contains(event.getAlertId()))
                .toList();
    }

    /**
     * Batch insert into processed_alerts. No flush here — rows are written with the
     * rest of the transaction so Hibernate can batch them.
     */
    public void markProcessed(Collection<UUID> alertIds) {
        List<ProcessedAlert> rows = alertIds.stream()
                .map(id -> ProcessedAlert.builder().alertId(id).build())
                .toList();
        processedAlertRepository.saveAll(rows);
        log.debug("Marked processed: {} alerts", rows.size());
    }
}