package com.financialsurveillance.activitymonitor.rules.layeringspoofing;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Rule009LayeringSpoofing implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // TODO: Implement when user-service REST client is ready
        // Rule: Look at recent cancelled orders for the same advisor + same symbol within a short time window.
        // If the count of cancellations exceeds a threshold (e.g. 3+ cancellations) relative to actual executions → suspicious
        // Need a separate event type — something like OrderCancelledEvent — to detect this pattern.
        // Severity is CRITICAL
        return Optional.empty();
    }
}
