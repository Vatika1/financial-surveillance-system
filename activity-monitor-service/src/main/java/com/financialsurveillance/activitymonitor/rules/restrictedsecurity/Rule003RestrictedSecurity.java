package com.financialsurveillance.activitymonitor.rules.restrictedsecurity;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class Rule003RestrictedSecurity implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        Set<String> restrictedSymbols = context.getRestrictedSymbols();
        if(!restrictedSymbols.contains(event.getSymbol())){
            return Optional.empty();
        }
        return Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_003")
                .ruleName("Restricted Security")
                .severity(AlertSeverity.CRITICAL)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Traded symbol " + event.getSymbol() + " is in the restricted list")
                .violationDetails(null)
                .build());
    }
}
