package com.financialsurveillance.activitymonitor.rules.washtrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class Rule008WashTrading implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        List<TradeCreatedEvent> filteredTradesBySymbol = context.getRecentTrades().stream()
                .filter(t -> t.getSymbol().equals(event.getSymbol()) )
                .toList();

        boolean hasSell = filteredTradesBySymbol.stream().anyMatch(t -> t.getTradeType().equals(TradeType.SELL));
        boolean hasBuy = filteredTradesBySymbol.stream().anyMatch(t -> t.getTradeType().equals(TradeType.BUY));

        return hasBuy && hasSell
                ? Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_008")
                .ruleName("Wash Trading")
                .severity(AlertSeverity.HIGH)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Advisor executed trade for " + event.getSymbol() + " which is Wash Trading")
                .violationDetails(null)       // null for now
                .build())  : Optional.empty();
    }
}
