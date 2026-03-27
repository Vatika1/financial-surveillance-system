package com.financialsurveillance.activitymonitor.service;


import com.financialsurveillance.activitymonitor.cache.TradeWindowStore;
import com.financialsurveillance.activitymonitor.domain.RuleViolation;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.engine.SurveillanceEngine;
import com.financialsurveillance.activitymonitor.mapper.RuleViolationMapper;
import com.financialsurveillance.activitymonitor.producer.AlertEventProducer;
import com.financialsurveillance.activitymonitor.repository.RuleViolationRepository;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActivityMonitorServiceTest {
    @Mock
    private RuleViolationRepository ruleViolationRepository;
    @Mock
    private TradeWindowStore tradeWindowStore;
    @Mock
    private SurveillanceEngine surveillanceEngine;
    @Mock
    private AlertEventProducer alertEventProducer;

    private final static Set<String> RESTRICTED_SYMBOLS = Set.of("AAPL", "TSLA", "MSFT", "GOOGL", "AMZN");
    @Mock
    private RuleViolationMapper ruleViolationMapper;
    @InjectMocks
    private ActivityMonitorService activityMonitorService;

    private TradeCreatedEvent getTradeCreatedEvent() {
        return TradeCreatedEvent.builder()
                .tradeId("TRD-100")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CL-001")
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(150.00))
                .totalValue(BigDecimal.valueOf(400000))
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();
    }
    @Test
    void processTrade_ShouldSuccessfullyProcessTrade(){
        // Create once at the top of the test
        TradeCreatedEvent event = getTradeCreatedEvent();
        // Arrange - set up mocks
        TradeCreatedEvent t1 = TradeCreatedEvent.builder()
                .tradeId("TRD-200")
                .advisorId("ADV-002")
                .accountId("ACC-002")
                .clientId("CL-002")
                .symbol("TSLA")
                .tradeType(TradeType.SELL)
                .quantity(BigDecimal.valueOf(500))
                .price(BigDecimal.valueOf(1200.00))
                .totalValue(BigDecimal.valueOf(600000))
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

        TradeCreatedEvent t2 = TradeCreatedEvent.builder()
                .tradeId("TRD-300")
                .advisorId("ADV-003")
                .accountId("ACC-003")
                .clientId("CL-003")
                .symbol("MSFT")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(200))
                .price(BigDecimal.valueOf(300.00))
                .totalValue(BigDecimal.valueOf(60000))
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.of(2026, 3, 27, 17, 0, 0, 0, ZoneId.of("America/New_York")))
                .createdAt(ZonedDateTime.now())
                .build();

        RuleViolationDTO violationDTO = RuleViolationDTO.builder()
                .ruleId("RULE_008")
                .ruleName("Wash Trading")
                .severity(AlertSeverity.HIGH)
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();

        RuleViolation violationEntity = RuleViolation.builder()
                .ruleId("RULE_008")
                .ruleName("Wash Trading")
                .advisorId("ADV-001")
                .tradeId("TRD-001")
                .build();
        when(tradeWindowStore.getRecentTrades(event.getAdvisorId(), Duration.ofSeconds(60)))
                .thenReturn(List.of(t1, t2));
        when(surveillanceEngine.evaluate(any(), any()))
                .thenReturn(List.of(violationDTO));
        when(ruleViolationMapper.toEntity(violationDTO)).thenReturn(violationEntity);

        // Act
        activityMonitorService.processTrade(event);
        // Assert
        verify(tradeWindowStore).addTrade(event.getAdvisorId(), event);
        verify(ruleViolationRepository).save(violationEntity);
        verify(alertEventProducer).publishAlert(violationDTO, event);
    }

    @Test
    void processTrade_ShouldFailToProcessTrade(){

    }
}
