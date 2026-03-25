package com.financialsurveillance.activitymonitor.rules.concentrationrisk;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Rule010ConcentrationRisk implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // TODO: Implement when user-service REST client is ready
        // You know the trade value from the current TradeCreatedEvent
        // You need the total account/portfolio size from user-service
        // Calculate: tradeValue / portfolioSize — if that ratio exceeds a threshold (e.g. 20%) → flag it
        // Needs REST call to user-service to fetch portfolio/account size by accountId
        // Severity is MEDIUM
        return Optional.empty();
    }
}
