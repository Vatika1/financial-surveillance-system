package com.financialsurveillance.alertservice.service;

import com.financialsurveillance.alertservice.domain.ProcessedAlert;
import com.financialsurveillance.alertservice.repository.ProcessedAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyService {

    private final ProcessedAlertRepository processedAlertRepository;
    public void markProcessed(UUID alertId){
        ProcessedAlert processedAlert = ProcessedAlert.builder()
                .alertId(alertId)
                .build();
        processedAlertRepository.save(processedAlert);
        log.debug("Marked processed: alertId={}", alertId);
    }

}
