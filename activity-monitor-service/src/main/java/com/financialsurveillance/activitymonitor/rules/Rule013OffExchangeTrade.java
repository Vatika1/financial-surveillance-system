package com.financialsurveillance.activitymonitor.rules;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Rule013OffExchangeTrade implements SurveillanceRule{
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        // BUG (planted): exchange is optional in TradeRequest, so this can be null
        if (!event.getExchange().equals("OTC")) {
            return Optional.empty();
        }

        return Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_013")
                .ruleName("Off-Exchange Trade")
                .severity(AlertSeverity.MEDIUM)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Trade executed off-exchange (OTC)")
                .violationDetails(null)
                .build());
    }
}
