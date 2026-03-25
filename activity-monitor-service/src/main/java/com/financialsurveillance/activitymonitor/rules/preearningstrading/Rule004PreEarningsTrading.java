package com.financialsurveillance.activitymonitor.rules.preearningstrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class Rule004PreEarningsTrading implements SurveillanceRule {

    private static final Set<String> EARNINGS_WATCH_SYMBOLS =
            Set.of("AAPL", "TSLA", "MSFT", "GOOGL", "AMZN");
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        if(!EARNINGS_WATCH_SYMBOLS.contains(event.getSymbol())){
            return Optional.empty();
        }
        return Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_004")
                .ruleName("Pre-Earnings Trading")
                .severity(AlertSeverity.HIGH)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Advisor " + event.getAdvisorId() + " traded " + event.getSymbol() + " which is on the earnings watch list")
                .violationDetails(null)
                .build());
    }
}
