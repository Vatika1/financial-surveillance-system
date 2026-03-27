package com.financialsurveillance.activitymonitor.rules.largetrade;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
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
public class Rule001LargeTradeSizeTest {

    @InjectMocks
    private Rule001LargeTradeSize rule;

    private RuleContext ruleContext;

    @BeforeEach
    void setUp() {
        ruleContext = RuleContext.builder()
                .recentTrades(List.of())
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
    }

    @Test
    void shouldDetectLargeTrading_tradeGreaterThan500k(){
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .totalValue(BigDecimal.valueOf(600000))
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isPresent());
    }
    @Test
    void shouldNotDetectLargeTrading_tradeLessThan500k(){
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .totalValue(BigDecimal.valueOf(300000))
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty());

    }
    @Test
    void shouldDetectLargeTrading_tradeEqualTo500k(){
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .totalValue(BigDecimal.valueOf(500000))
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isPresent());

    }
}
