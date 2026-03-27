package com.financialsurveillance.activitymonitor.rules.restrictedsecurity;

import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class Rule003RestrictedSecurityTest {

    @InjectMocks
    private Rule003RestrictedSecurity rule;
    private static final Set<String> RESTRICTED_SYMBOLS =
            Set.of("AAPL", "TSLA");

    @Test
    void shouldDetectRestrictedSecurity_whenRestrictedSymbol(){
        RuleContext context = RuleContext.builder()
                .restrictedSymbols(RESTRICTED_SYMBOLS)
                .accountSize(Optional.empty())
                .recentTrades(List.of())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("AAPL")
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotDetectRestrictedSecurity_whenNoRestrictedSymbol(){
        RuleContext context = RuleContext.builder()
                .restrictedSymbols(RESTRICTED_SYMBOLS)
                .accountSize(Optional.empty())
                .recentTrades(List.of())
                .build();
        TradeCreatedEvent currentEvent = TradeCreatedEvent.builder()
                .symbol("MSFT")
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        Optional<RuleViolationDTO> result = rule.evaluate(currentEvent, context);
        assertTrue(result.isEmpty());
    }
}
