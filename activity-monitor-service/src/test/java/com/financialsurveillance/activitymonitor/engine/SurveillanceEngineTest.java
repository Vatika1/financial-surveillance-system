package com.financialsurveillance.activitymonitor.engine;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.rules.SurveillanceRule;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SurveillanceEngineTest {

    private RuleContext ruleContext;

    @BeforeEach
    void setUp() {
        ruleContext = RuleContext.builder()
                .recentTrades(List.of())
                .restrictedSymbols(Set.of())
                .accountSize(Optional.empty())
                .build();
    }

    private TradeCreatedEvent getTradeCreatedEvent(){
        return TradeCreatedEvent.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    void evaluateTrade_ShouldDetectViolation(){
        TradeCreatedEvent event = getTradeCreatedEvent();
        RuleViolationDTO violation = RuleViolationDTO.builder()
                .ruleId("RULE_001")
                .ruleName("Large Trade Size")
                .severity(AlertSeverity.HIGH)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .violationDescription("Trade value exceeds threshold")
                .violationDetails(null)
                .build();

        SurveillanceRule mockRule = mock(SurveillanceRule.class);
        when(mockRule.evaluate(any(), any())).thenReturn(Optional.of(violation));
        SurveillanceEngine engine = new SurveillanceEngine(List.of(mockRule));

        List<RuleViolationDTO> result = engine.evaluate(event, ruleContext);

        assertEquals(1, result.size());
        assertEquals("RULE_001", result.get(0).getRuleId());  // content check
        assertEquals(AlertSeverity.HIGH, result.get(0).getSeverity());  // content check

    }

    @Test
    void evaluateTrade_ShouldNotDetectViolation(){
        TradeCreatedEvent event = getTradeCreatedEvent();

        SurveillanceRule mockRule = mock(SurveillanceRule.class);
        when(mockRule.evaluate(any(), any())).thenReturn(Optional.empty());
        SurveillanceEngine engine = new SurveillanceEngine(List.of(mockRule));

        List<RuleViolationDTO> result = engine.evaluate(event, ruleContext);
        assertEquals(0, result.size());

    }
}
