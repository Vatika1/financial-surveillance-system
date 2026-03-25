package com.financialsurveillance.activitymonitor.rules.largetrade;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class Rule001LargeTradeSize implements SurveillanceRule {
    @Override
    public Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context) {
        if (event.getTotalValue().compareTo(BigDecimal.valueOf(500000)) < 0) {
            return Optional.empty();
        }

        return Optional.of(RuleViolationDTO.builder()
                .ruleId("RULE_001")
                .ruleName("Large Trade Size")
                .severity(AlertSeverity.HIGH)
                .advisorId(event.getAdvisorId())
                .tradeId(event.getTradeId())
                .violationDescription("Trade value of $" + event.getTotalValue() + " exceeds the $500,000 threshold")
                .violationDetails(null)       // null for now
                .build());
    }
    }

