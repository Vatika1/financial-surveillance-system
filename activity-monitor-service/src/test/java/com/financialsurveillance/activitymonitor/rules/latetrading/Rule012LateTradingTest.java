package com.financialsurveillance.activitymonitor.rules.latetrading;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class Rule012LateTradingTest {

    @InjectMocks
    private Rule012LateTrading rule;
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
    void shouldDetectLateTrading_tradeAfter4(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 27,   // date
                17, 0, 0, 0,   // 5 PM
                ZoneId.of("America/New_York")
        );
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeTimestamp(zdt)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotDetectLateTrading_tradeBefore4(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 27,   // date
                14, 0, 0, 0,   // 3 PM
                ZoneId.of("America/New_York")
        );
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeTimestamp(zdt)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty());

    }

    @Test
    void shouldNotDetectLateTrading_tradeAt4(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 27,   // date
                16, 0, 0, 0,   // 4 PM
                ZoneId.of("America/New_York")
        );
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeTimestamp(zdt)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty());

    }
}
