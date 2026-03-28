package com.financialsurveillance.tradingestion.service;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeStatus;
import com.financialsurveillance.events.TradeType;
import com.financialsurveillance.tradingestion.domain.Trade;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.mapper.TradeMapper;
import com.financialsurveillance.tradingestion.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeIngestionServiceTest {
    @InjectMocks
    private TradeIngestionService tradeIngestionService;

    @Mock
    private KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private TradeMapper tradeMapper;

    @Value("${kafka.topics.trades-raw}")
    private String topic;

    private TradeRequest tradeRequest;

    private static final Set<String> symbols = new HashSet<>(Set.of(
            "AAPL", "MSFT", "JPM", "GS", "TSLA",
            "AMZN", "GOOGL", "META", "BAC", "WFC"
    ));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tradeIngestionService, "topic", "trades.raw");
    }
    private TradeRequest getTradeRequest() {
        return TradeRequest.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CL-001")
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(150.00))
                .currency("USD")
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now())
                .sourceSystem("ETRADE")
                .sourceSystemId("SYS-001")
                .build();
    }

    @Test
    void processTradeRequest_ShouldSuccessfullyProcessTrade(){
        TradeRequest request = getTradeRequest();
        Trade trade = Trade.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CL-001")
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(150.00))
                .totalValue(BigDecimal.valueOf(15000))
                .currency("USD")
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now())
                .sourceSystem("ETRADE")
                .sourceSystemId("SYS-001")
                .status(TradeStatus.RECEIVED)
                .createdAt(ZonedDateTime.now())
                .build();

        TradeCreatedEvent event = TradeCreatedEvent.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CL-001")
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(150.00))
                .totalValue(BigDecimal.valueOf(15000))
                .currency("USD")
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now())
                .sourceSystem("ETRADE")
                .sourceSystemId("SYS-001")
                .status(TradeStatus.RECEIVED)
                .createdAt(ZonedDateTime.now())
                .build();

        TradeResponse response = TradeResponse.builder()
                .id(UUID.randomUUID())
                .tradeId("TRD-001")
                .status(TradeStatus.RECEIVED)
                .createdAt(ZonedDateTime.now())
                .build();
        when(tradeMapper.toEntity(request)).thenReturn(trade);
        when(tradeRepository.save(trade)).thenReturn(trade);
        when(tradeMapper.toTradeCreatedEvent(trade)).thenReturn(event);
        when(tradeMapper.toResponseDto(trade)).thenReturn(response);

        TradeResponse result = tradeIngestionService.processTrade(request);

        assertNotNull(response);
        assertEquals("TRD-001", result.getTradeId());
        assertEquals(TradeStatus.RECEIVED, result.getStatus());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());

        verify(tradeRepository).save(trade);
        verify(tradeMapper).toTradeCreatedEvent(trade);
        verify(kafkaTemplate).send(any(), eq(trade.getAdvisorId()), eq(event));
    }

    @Test
    void processTradeRequest_ShouldThrowDuplicateTradeException_whenTradeAlreadyExists(){
        TradeRequest request = getTradeRequest();
        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(true);

        assertThrows(DuplicateTradeException.class, () -> {
            tradeIngestionService.processTrade(request);
        });

        verify(tradeRepository).existsByTradeId(request.getTradeId());
    }

    @Test
    void processTradeRequest_ShouldThrowInvalidTradeException_whenTimestampTooOld(){
        ZonedDateTime timestamp = ZonedDateTime.now().minusDays(4);
        TradeRequest request = getTradeRequest();
        request.setTradeTimestamp(timestamp);

        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(false);
        assertThrows(InvalidTradeException.class, () -> {
            tradeIngestionService.processTrade(request);
        });
    }

    @Test
    void processTradeRequest_ShouldThrowInvalidTradeException_whenInvalidSymbol(){
        TradeRequest request = getTradeRequest();
        request.setSymbol("XYZ");

        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(false);
        assertThrows(InvalidTradeException.class, () -> {
            tradeIngestionService.processTrade(request);
        });

    }
}
