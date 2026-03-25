package com.financialsurveillance.activitymonitor.rules.afterhoursspike;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class Rule006AfterHoursSpike implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {

        ZonedDateTime easternTime = event.getTradeTimestamp()
                .withZoneSameInstant(ZoneId.of("America/New_York"));

        return (easternTime.toLocalTime().isBefore(LocalTime.of(9, 30)) ||
                easternTime.toLocalTime().isAfter(LocalTime.of(16, 0)))
                ? Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_006")
                .ruleName("After Hours Spike")
                .severity(AlertSeverity.MEDIUM)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Advisor traded at " + event.getTradeTimestamp() + " during after hours")
                .violationDetails(null)
                .build())
                : Optional.empty();
    }
}
