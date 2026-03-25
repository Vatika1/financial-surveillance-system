package com.financialsurveillance.activitymonitor.rules;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;

import java.util.Optional;

public interface SurveillanceRule {
    Optional<RuleViolationDTO> evaluate(TradeCreatedEvent event, RuleContext context);
}
