package com.financialsurveillance.activitymonitor.service;

import com.financialsurveillance.activitymonitor.domain.ProcessedEvent;
import com.financialsurveillance.activitymonitor.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyService {
    private final ProcessedEventRepository processedEventRepository;

    public void markProcessed(String tradeId){
        ProcessedEvent event = ProcessedEvent.builder()
                .tradeId(tradeId)
                .build();
        processedEventRepository.save(event);
        log.debug("Marked processed: tradeId={}", tradeId);
    }
}
