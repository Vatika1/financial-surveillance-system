package com.financialsurveillance.alertservice.service;

import com.financialsurveillance.alertservice.domain.Alert;
import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.alertservice.mapper.AlertMapper;
import com.financialsurveillance.alertservice.producer.AlertPersistedEventProducer;
import com.financialsurveillance.alertservice.repository.AlertRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertService {

    private final AlertMapper alertMapper;
    private final AlertRepository alertRepository;
    private final AlertPersistedEventProducer alertPersistedEventProducer;
    private final EntityManager entityManager;


    @Transactional
    public void processAlerts(List<AlertCreatedEvent> events){
        log.info("Processing {} alerts from alert batch", events.size());

        ZonedDateTime now = ZonedDateTime.now();
        List<AlertPersistedEvent> persistedEvents = new ArrayList<>(events.size());

        for (AlertCreatedEvent event : events){
            log.debug("Persisting alert alertId={} ruleId={} advisorId={}",
                    event.getAlertId(), event.getRuleId(), event.getAdvisorId());
            Alert persistedAlert = Alert.builder()
                    .alertId(event.getAlertId())
                    .alertTypeId(event.getAlertTypeId())
                    .severity(event.getSeverity())
                    .tradeId(event.getTradeId())
                    .status(event.getStatus())
                    .violationDetails(event.getViolationDetails())
                    .ruleName(event.getRuleName())
                    .ruleId(event.getRuleId())
                    .advisorId(event.getAdvisorId())
                    .createdAt(event.getCreatedAt())
                    .build();

            entityManager.persist(persistedAlert);

            persistedEvents.add(
                    AlertPersistedEvent.builder()
                            .alertTypeId(persistedAlert.getAlertTypeId())
                            .ruleId(persistedAlert.getRuleId())
                            .severity(persistedAlert.getSeverity())
                            .alertId(persistedAlert.getAlertId())
                            .tradeId(persistedAlert.getTradeId())
                            .advisorId(persistedAlert.getAdvisorId())
                            .violationDetails(persistedAlert.getViolationDetails())
                            .ruleName(persistedAlert.getRuleName())
                            .status(persistedAlert.getStatus())
                            .persistedAt(now)
                            .createdAt(persistedAlert.getCreatedAt())
                            .build()
            );
        }

        log.info("Persisted alerts {} ", persistedEvents.size());
            alertPersistedEventProducer.publishAll(persistedEvents);
    }

    public void processAlert(AlertCreatedEvent event){
        log.info("Processing Alert alertId={} alertTypeId={} tradeId={} advisorId={}",
                event.getAlertId(), event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

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
