package com.financialsurveillance.activitymonitor.rules.afterhoursspike;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.latetrading.Rule012LateTrading;
import com.financialsurveillance.events.TradeCreatedEvent;
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
public class Rule006AfterHoursSpikeTest {

    @InjectMocks
    private Rule006AfterHoursSpike rule;

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
    void shouldDetectAfterHoursSpike_tradeAt6PM(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 29,   // date
                18, 0, 0, 0,   // 6 PM
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
    void shouldDetectAfterHoursSpike_tradeAt6AM(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 29,   // date
                6, 0, 0, 0,   // 6 AM
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
    void shouldNotDetectAfterHoursSpike_tradeAt1pm(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 29,   // date
                13, 0, 0, 0,   // 1 PM
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
    void shouldNotDetectAfterHoursSpike_tradeAtMarketOpen_9_30AM(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 29,
                9, 30, 0, 0,
                ZoneId.of("America/New_York")
        );
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeTimestamp(zdt)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty()); // exactly 9:30 = market hours
    }

    @Test
    void shouldNotDetectAfterHoursSpike_tradeAtMarketClose_4_30PM(){
        ZonedDateTime zdt = ZonedDateTime.of(
                2026, 3, 29,
                9, 30, 0, 0,
                ZoneId.of("America/New_York")
        );
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .tradeTimestamp(zdt)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, ruleContext);
        assertTrue(result.isEmpty()); // exactly 9:30 = market hours
    }
}
