package com.financialsurveillance.activitymonitor.rules.frontrunning;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Rule007FrontRunning implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // TODO: Implement when user-service REST client is ready
        // Rule: Detect when an advisor trades a symbol for their own account shortly before a large client order comes in for the same symbol
        //Check if the current trade's symbol exists in that pending orders HashSet
        //If it does → the advisor is trading a symbol that has a large client order queued for it → flag as front-running
        // Needs: context.getAccountSize() from user-service. Severity is CRITICAL
        //usage of hashset
        return Optional.empty();
    }
}
