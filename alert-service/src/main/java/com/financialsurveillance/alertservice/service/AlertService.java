package com.financialsurveillance.alertservice.service;

import com.financialsurveillance.alertservice.domain.Alert;
import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.alertservice.mapper.AlertMapper;
import com.financialsurveillance.alertservice.producer.AlertPersistedEventProducer;
import com.financialsurveillance.alertservice.repository.AlertRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertService {

    private final AlertMapper alertMapper;
    private final AlertRepository alertRepository;
    private final AlertPersistedEventProducer alertPersistedEventProducer;

    @Transactional
    public void processAlert(AlertCreatedEvent event){
        log.info("Processing Alert alertId={} alertTypeId={} tradeId={} advisorId={}",
                event.getAlertId(), event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());
        if(alertRepository.existsByAlertId(event.getAlertId())){
            log.warn("Duplicate alert alertId={} alertTypeId={} tradeId={} advisorId={}", event.getAlertId(),
                    event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());
            return;
        }
        AlertDTO dto = AlertDTO.builder()
                .severity(event.getSeverity())
                .alertTypeId(event.getAlertTypeId())
                .alertId(event.getAlertId())
                .tradeId(event.getTradeId())
                .status(event.getStatus())
                .advisorId(event.getAdvisorId())
                .createdAt(event.getCreatedAt())
                .violationDetails(event.getViolationDetails())
                .ruleId(event.getRuleId())
                .ruleName(event.getRuleName())
                .build();
        Alert alert = alertMapper.toEntity(dto);
        alertRepository.save(alert);
        alertPersistedEventProducer.publishAlert(dto, event);
    }
}
