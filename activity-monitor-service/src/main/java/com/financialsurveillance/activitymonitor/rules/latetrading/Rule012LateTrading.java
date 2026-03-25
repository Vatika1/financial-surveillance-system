package com.financialsurveillance.activitymonitor.rules.latetrading;

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
public class Rule012LateTrading implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {

        ZonedDateTime easternTime = event.getTradeTimestamp()
                .withZoneSameInstant(ZoneId.of("America/New_York"));

        return (easternTime.toLocalTime().isAfter(LocalTime.of(16, 0)))
                ? Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_012")
                .ruleName("Late Trading")
                .severity(AlertSeverity.HIGH)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Advisor traded at " + easternTime + " during after hours")
                .violationDetails(null)
                .build())
                : Optional.empty();
    }
}
