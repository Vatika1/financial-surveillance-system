package com.financialsurveillance.alertservice.mapper;

import com.financialsurveillance.alertservice.domain.Alert;
import com.financialsurveillance.alertservice.dto.AlertDTO;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class AlertMapper {

    public Alert toEntity(AlertDTO dto){

        return Alert.builder()
                .alertId(dto.getAlertId())
                .severity(dto.getSeverity())
                .violationDetails(dto.getViolationDetails())
                .alertTypeId(dto.getAlertTypeId())
                .advisorId(dto.getAdvisorId())
                .createdAt(dto.getCreatedAt())
                .ruleId(dto.getRuleId())
                .ruleName(dto.getRuleName())
                .status(dto.getStatus()).tradeId(dto.getTradeId())
                .build();
    }
}
