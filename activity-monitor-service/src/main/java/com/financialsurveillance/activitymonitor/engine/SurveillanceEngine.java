package com.financialsurveillance.activitymonitor.engine;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class SurveillanceEngine {

    private final List<SurveillanceRule> rules;

    public List<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context){

        return rules.stream()
                .map(rule -> rule.evaluate(event, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
