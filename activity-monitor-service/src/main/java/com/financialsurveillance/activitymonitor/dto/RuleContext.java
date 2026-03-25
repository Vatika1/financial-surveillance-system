package com.financialsurveillance.activitymonitor.dto;

import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Builder
@Getter
public class RuleContext {

    private final List<TradeCreatedEvent> recentTrades;
    private final Set<String> restrictedSymbols;
    private final Optional<BigDecimal> accountSize;


}
