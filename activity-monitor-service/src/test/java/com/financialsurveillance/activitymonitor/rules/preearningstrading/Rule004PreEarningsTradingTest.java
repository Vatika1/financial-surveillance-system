package com.financialsurveillance.activitymonitor.rules.preearningstrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.latetrading.Rule012LateTrading;
import com.financialsurveillance.events.TradeCreatedEvent;
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
public class Rule004PreEarningsTradingTest {

    @InjectMocks
    private Rule004PreEarningsTrading rule;

    private RuleContext ruleContext;
    private static final Set<String> EARNINGS_WATCH_SYMBOLS =
            Set.of("AAPL", "TSLA", "MSFT", "GOOGL", "AMZN");

    @BeforeEach
    void setUp() {
        ruleContext = RuleContext.builder()
                .recentTrades(List.of())
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
    }
    @Test
    void shouldDetectPreEarningsTrading_whenTradingFromEarningsWatchSymbol(){
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isPresent());
    }
    @Test
    void shouldNotDetectPreEarningsTrading_whenNotTradingFromEarningsWatchSymbol(){
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("TSX")
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty());

    }
}
