package com.financialsurveillance.activitymonitor.service;

import com.financialsurveillance.activitymonitor.domain.ProcessedEvent;
import com.financialsurveillance.activitymonitor.repository.ProcessedEventRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyService {
    private final ProcessedEventRepository processedEventRepository;


    public List<TradeCreatedEvent> filterUnprocessed(List<TradeCreatedEvent> events){
        Map<String, TradeCreatedEvent> byTradeId = new LinkedHashMap<>();
        for (TradeCreatedEvent event : events) {
            byTradeId.putIfAbsent(event.getTradeId(), event);
        }

        Set<String> alreadyProcessed = processedEventRepository.findAllById(byTradeId.keySet()).stream()
                .map(ProcessedEvent:: getTradeId)
                .collect(Collectors.toSet());

        int skipped = events.size() - byTradeId.size() + alreadyProcessed.size();
        if (skipped > 0) {
            log.warn("Duplicate trade detected: skipping {} of {} in batch", skipped, events.size());
        }

        return byTradeId.values().stream()
                .filter(event -> !alreadyProcessed.contains(event.getTradeId()))
                .toList();
    }

    public void markProcessed(Collection<String> tradeIds) {
        List<ProcessedEvent> rows = tradeIds.stream()
                .map(id -> ProcessedEvent.builder().tradeId(id).build())
                .toList();
        processedEventRepository.saveAll(rows);
        log.debug("Marked processed: {} trades", rows.size());
    }

    public void markProcessed(String tradeId){
        ProcessedEvent event = ProcessedEvent.builder()
                .tradeId(tradeId)
                .build();
        processedEventRepository.save(event);
        log.debug("Marked processed: tradeId={}", tradeId);
    }
}
