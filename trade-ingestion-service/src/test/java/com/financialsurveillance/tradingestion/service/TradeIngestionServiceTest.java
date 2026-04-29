package com.financialsurveillance.tradingestion.service;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeStatus;
import com.financialsurveillance.events.TradeType;
import com.financialsurveillance.tradingestion.domain.Trade;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.exception.TradePublishException;
import com.financialsurveillance.tradingestion.mapper.TradeMapper;
import com.financialsurveillance.tradingestion.producer.TradeEventProducer;
import com.financialsurveillance.tradingestion.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeIngestionServiceTest {

    @InjectMocks
    private TradeIngestionService tradeIngestionService;

    @Mock private TradeRepository tradeRepository;
    @Mock private TradeMapper tradeMapper;
    @Mock private TradeEventProducer tradeEventProducer;

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

    private Trade getTrade() {
        return Trade.builder()
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
    }

    private TradeCreatedEvent getEvent() {
        return TradeCreatedEvent.builder()
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
    }

    private TradeResponse getResponse() {
        return TradeResponse.builder()
                .id(UUID.randomUUID())
                .tradeId("TRD-001")
                .status(TradeStatus.RECEIVED)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    @Test
    void processTradeRequest_ShouldSuccessfullyProcessTrade() {
        TradeRequest request = getTradeRequest();
        Trade trade = getTrade();
        TradeCreatedEvent event = getEvent();
        TradeResponse response = getResponse();

        when(tradeMapper.toEntity(request)).thenReturn(trade);
        when(tradeRepository.save(trade)).thenReturn(trade);
        when(tradeMapper.toTradeCreatedEvent(trade)).thenReturn(event);
        when(tradeMapper.toResponseDto(trade)).thenReturn(response);

        TradeResponse result = tradeIngestionService.processTrade(request);

        assertNotNull(result);
        assertEquals("TRD-001", result.getTradeId());
        assertEquals(TradeStatus.RECEIVED, result.getStatus());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());

        verify(tradeRepository).save(trade);
        verify(tradeMapper).toTradeCreatedEvent(trade);
        verify(tradeEventProducer).publishTradeCreated(event);
    }

    @Test
    void processTradeRequest_ShouldThrowDuplicateTradeException_whenTradeAlreadyExists() {
        TradeRequest request = getTradeRequest();
        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(true);

        assertThrows(DuplicateTradeException.class,
                () -> tradeIngestionService.processTrade(request));

        verify(tradeRepository).existsByTradeId(request.getTradeId());
        verify(tradeRepository, never()).save(any());
        verify(tradeEventProducer, never()).publishTradeCreated(any());
    }

    @Test
    void processTradeRequest_ShouldThrowInvalidTradeException_whenTimestampTooOld() {
        TradeRequest request = getTradeRequest();
        request.setTradeTimestamp(ZonedDateTime.now().minusDays(4));
        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(false);

        assertThrows(InvalidTradeException.class,
                () -> tradeIngestionService.processTrade(request));

        verify(tradeRepository, never()).save(any());
        verify(tradeEventProducer, never()).publishTradeCreated(any());
    }

    @Test
    void processTradeRequest_ShouldThrowInvalidTradeException_whenInvalidSymbol() {
        TradeRequest request = getTradeRequest();
        request.setSymbol("XYZ");
        when(tradeRepository.existsByTradeId(request.getTradeId())).thenReturn(false);

        assertThrows(InvalidTradeException.class,
                () -> tradeIngestionService.processTrade(request));

        verify(tradeRepository, never()).save(any());
        verify(tradeEventProducer, never()).publishTradeCreated(any());
    }

    @Test
    void processTradeRequest_ShouldPropagatePublishException_whenProducerFails() {
        TradeRequest request = getTradeRequest();
        Trade trade = getTrade();
        TradeCreatedEvent event = getEvent();

        when(tradeMapper.toEntity(request)).thenReturn(trade);
        when(tradeRepository.save(trade)).thenReturn(trade);
        when(tradeMapper.toTradeCreatedEvent(trade)).thenReturn(event);
        doThrow(new TradePublishException("simulated kafka failure", new RuntimeException()))
                .when(tradeEventProducer).publishTradeCreated(event);

        assertThrows(TradePublishException.class,
                () -> tradeIngestionService.processTrade(request));

        verify(tradeRepository).save(trade);
        verify(tradeEventProducer).publishTradeCreated(event);
        // Note: Spring's @Transactional rollback is verified end-to-end in
        // integration tests, not here — unit tests only verify exception propagation.
    }
}