package com.financialsurveillance.activitymonitor.mapper;

import com.financialsurveillance.activitymonitor.domain.RuleViolation;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.AlertSeverity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;

@Component
public class RuleViolationMapper {

    public RuleViolation toEntity(RuleViolationDTO dto){

        return RuleViolation.builder()
                .ruleId(dto.getRuleId())
                .tradeId(dto.getTradeId())
                .violationDetails(dto.getViolationDetails())
                .ruleName(dto.getRuleName())
                .advisorId(dto.getAdvisorId())
                .severity(String.valueOf(dto.getSeverity()))
                .detectedAt(ZonedDateTime.now())
                .build();
    }
}
