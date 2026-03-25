package com.financialsurveillance.activitymonitor.rules.rapidrepeattrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class Rule002RapidRepeatTrading implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {

        long count = context.getRecentTrades().stream()
                .filter(t -> t.getSymbol().equals(event.getSymbol()))
                .count();

        if(count < 3){
            return Optional.empty();
        }
        return Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_002")
                .ruleName("Rapid Repeat Trading")
                .severity(AlertSeverity.CRITICAL)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Advisor " + event.getAdvisorId() +
                        " traded " + event.getSymbol() +
                        " " + count + " times within 60 seconds")
                .violationDetails(null)
                .build());

    }
}
