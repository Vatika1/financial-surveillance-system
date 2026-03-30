package com.financialsurveillance.activitymonitor.rules.rapidrepeattrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class Rule002RapidRepeatTradingTest {
    @InjectMocks
    private Rule002RapidRepeatTrading rule;

    private TradeCreatedEvent getTradeCreatedEvent(){
        return TradeCreatedEvent.builder()
                .symbol("AAPL")
                .totalValue(BigDecimal.valueOf(600000))
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
    }

    @Test
    void shouldDetectRapidRepeatTrading_recentTradesGreaterThan3(){
        TradeCreatedEvent currentEvent = getTradeCreatedEvent();
        List<TradeCreatedEvent> recentTrades = List.of(getTradeCreatedEvent(), getTradeCreatedEvent(),
                getTradeCreatedEvent(), getTradeCreatedEvent());

        RuleContext context = RuleContext.builder()
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .recentTrades(recentTrades)
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isPresent());
    }

    @Test
    void shouldDetectRapidRepeatTrading_recentTradesEqualTo3(){
        TradeCreatedEvent currentEvent = getTradeCreatedEvent();
        List<TradeCreatedEvent> recentTrades = List.of(getTradeCreatedEvent(),
                getTradeCreatedEvent(),
                getTradeCreatedEvent());
        RuleContext context = RuleContext.builder()
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .recentTrades(recentTrades)
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotDetectRapidRepeatTrading_recentTradesLessThan3(){
        TradeCreatedEvent currentEvent = getTradeCreatedEvent();
        List<TradeCreatedEvent> recentTrades = List.of(getTradeCreatedEvent(), getTradeCreatedEvent());
        RuleContext context = RuleContext.builder()
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .recentTrades(recentTrades)
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isEmpty());

    }

}
