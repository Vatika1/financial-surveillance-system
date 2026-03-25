package com.financialsurveillance.activitymonitor.rules.excessivetrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;

import java.util.Optional;

public class Rule005ExcessiveTrading implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // TODO: Implement when user-service REST client is ready
        // Rule: trade value exceeds 20% of account size
        // Needs: context.getAccountSize() from user-service
        return Optional.empty();
    }
}
