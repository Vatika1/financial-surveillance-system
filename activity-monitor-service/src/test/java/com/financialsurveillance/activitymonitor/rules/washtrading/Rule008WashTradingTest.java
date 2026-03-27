package com.financialsurveillance.activitymonitor.rules.washtrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class Rule008WashTradingTest {

    @InjectMocks
    private Rule008WashTrading rule;

    @Test
    void shouldDetectWashTrading_whenBuyAndSellSameSymbol() {
        // arrange - build context with BUY and SELL
        TradeCreatedEvent buyTrade = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .build();
        TradeCreatedEvent sellTrade = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.SELL)
                .build();

        List<TradeCreatedEvent> recentTrades = List.of(buyTrade, sellTrade);
        RuleContext context = RuleContext.builder()
                .recentTrades(recentTrades)
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        // act - call rule.evaluate(context)
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);

        // assert - result is present
        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotDetectViolation_whenOnlyBuy(){
        TradeCreatedEvent buyTrade = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .build();

        List<TradeCreatedEvent> recentTrades = List.of(buyTrade);
        RuleContext context = RuleContext.builder()
                .recentTrades(recentTrades)
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotDetectViolation_whenOnlySell(){
        TradeCreatedEvent sellTrade = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.SELL)
                .build();

        List<TradeCreatedEvent> recentTrades = List.of(sellTrade);
        RuleContext context = RuleContext.builder()
                .recentTrades(recentTrades)
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.SELL)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isEmpty());

    }

    @Test
    void shouldNotDetectViolation_whenEmptyTradeWindow(){

        //empty recent trades
        List<TradeCreatedEvent> recentTrades = List.of();
        RuleContext context = RuleContext.builder()
                .recentTrades(recentTrades)
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeType(TradeType.SELL)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isEmpty());
    }
}
