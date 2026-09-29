package com.financialsurveillance.alertservice.service;

import com.financialsurveillance.alertservice.domain.ProcessedAlert;
import com.financialsurveillance.alertservice.repository.ProcessedAlertRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyService {

    private final ProcessedAlertRepository processedAlertRepository;

    //unused method
    public void markProcessed(UUID alertId){
        ProcessedAlert processedAlert = ProcessedAlert.builder()
                .alertId(alertId)
                .build();
        processedAlertRepository.save(processedAlert);
        log.debug("Marked processed: alertId={}", alertId);
    }

    public List<AlertCreatedEvent> filterUnprocessed(List<AlertCreatedEvent> events){
        Map<UUID, AlertCreatedEvent> byAlertId = new LinkedHashMap<>();
        for (AlertCreatedEvent event : events) {
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

    public void markProcessed(Collection<UUID> alertIds) {
        List<ProcessedAlert> rows = alertIds.stream()
                .map(id -> ProcessedAlert.builder().alertId(id).build())
                .toList();
        processedAlertRepository.saveAll(rows);
        log.debug("Marked processed: {} alerts", rows.size());
    }

}
