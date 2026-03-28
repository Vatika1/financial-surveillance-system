package com.financialsurveillance.alertservice.service;

import com.financialsurveillance.alertservice.domain.Alert;
import com.financialsurveillance.alertservice.dto.AlertDTO;
import com.financialsurveillance.alertservice.mapper.AlertMapper;
import com.financialsurveillance.alertservice.producer.AlertPersistedEventProducer;
import com.financialsurveillance.alertservice.repository.AlertRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private AlertMapper alertMapper;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertPersistedEventProducer alertPersistedEventProducer;
    @InjectMocks
    private AlertService alertService;

    private AlertCreatedEvent getAlertCreatedEvent() {
        return AlertCreatedEvent.builder()
                .alertId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .alertTypeId("AHS-20260327143022829")
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .ruleId("RULE_006")
                .ruleName("After Hours Spike")
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .createdAt(ZonedDateTime.now())
                .violationDetails(null)
                .build();
    }
    private AlertDTO eventToDto(AlertCreatedEvent event){
       return AlertDTO.builder()
                .alertId(event.getAlertId())
                .alertTypeId(event.getAlertTypeId())
                .tradeId(event.getTradeId())
                .advisorId(event.getAdvisorId())
                .ruleId(event.getRuleId())
                .ruleName(event.getRuleName())
                .severity(event.getSeverity())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .violationDetails(event.getViolationDetails())
                .build();
    }
    private Alert dtoToEntity(AlertDTO dto){
        return Alert.builder()
                .alertId(dto.getAlertId())
                .alertTypeId(dto.getAlertTypeId())
                .tradeId(dto.getTradeId())
                .advisorId(dto.getAdvisorId())
                .ruleName(dto.getRuleName())
                .ruleId(dto.getRuleId())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .violationDetails(dto.getViolationDetails())
                .build();
    }

    @Test
    void processAlert_ShouldSuccessfullyProcessAlert(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        AlertDTO dto = eventToDto(event);

        Alert alert = dtoToEntity(dto);
        when(alertRepository.existsByAlertId(event.getAlertId())).thenReturn(false);
        when(alertMapper.toEntity(any())).thenReturn(alert);

        alertService.processAlert(event);

        verify(alertRepository).save(alert);
        verify(alertPersistedEventProducer).publishAlert(any(), eq(event));
    }

    @Test
    void processAlert_ShouldFail_whenDuplicateAlert(){
        AlertCreatedEvent event = getAlertCreatedEvent();
        when(alertRepository.existsByAlertId(event.getAlertId())).thenReturn(true);

        alertService.processAlert(event);

        verify(alertRepository, never()).save(any());
        verify(alertPersistedEventProducer, never()).publishAlert(any(), any());
    }

}
