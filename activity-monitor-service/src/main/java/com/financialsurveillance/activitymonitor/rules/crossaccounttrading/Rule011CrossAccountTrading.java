package com.financialsurveillance.activitymonitor.rules.crossaccounttrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Rule011CrossAccountTrading implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // TODO: Implement when user-service REST client is ready
        // Requires user-service to tell you which accounts belong to which advisor.
        // Group recent trades by advisorId across all their managed accounts
        // Look for the same symbol being traded in the same direction across multiple accounts in a short window
        // High count of accounts trading same symbol simultaneously → suspicious coordination
        // Severity is HIGH
        return Optional.empty();
    }
}
