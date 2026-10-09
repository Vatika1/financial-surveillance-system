package com.financialsurveillance.tradingestion.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.events.TradeStatus;
import com.financialsurveillance.events.TradeType;
import com.financialsurveillance.tradingestion.domain.OutboxEvent;
import com.financialsurveillance.tradingestion.exception.TradePublishException;
import com.financialsurveillance.tradingestion.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxPollerTest {

    @InjectMocks
    private OutboxPoller outboxPoller;

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Mock
    private TradeEventProducer producer;

    private OutboxEvent getOutboxEvent(String tradeId, String headers) throws JsonProcessingException {
        TradeCreatedEvent event = TradeCreatedEvent.builder()
                .tradeId(tradeId)
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

        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(tradeId)
                .topic("trades.raw")
                .payload(objectMapper.writeValueAsString(event))
                .headers(headers)
                .createdAt(ZonedDateTime.now())
                .sentAt(null)
                .build();
    }

    @Test
    void publishOutbox_ShouldPublishAndMarkSent_whenUnsentRowsExist() throws JsonProcessingException {
        OutboxEvent row1 = getOutboxEvent("TRD-001", "{\"correlationId\":\"cid-1\"}");
        OutboxEvent row2 = getOutboxEvent("TRD-002", "{\"correlationId\":\"cid-2\"}");

        when(outboxEventRepository.findUnsentBatch()).thenReturn(List.of(row1, row2));

        outboxPoller.publishOutbox();

        ArgumentCaptor<TradeCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TradeCreatedEvent.class);
        ArgumentCaptor<String> cidCaptor = ArgumentCaptor.forClass(String.class);
        verify(producer, times(2)).publishTradeCreated(eventCaptor.capture(), cidCaptor.capture());

        assertEquals("TRD-001", eventCaptor.getAllValues().get(0).getTradeId());
        assertEquals("TRD-002", eventCaptor.getAllValues().get(1).getTradeId());
        assertEquals(List.of("cid-1", "cid-2"), cidCaptor.getAllValues());
        assertNotNull(row1.getSentAt());
        assertNotNull(row2.getSentAt());
    }

    @Test
    void publishOutbox_ShouldNotPublish_whenNoUnsentRows() throws JsonProcessingException {
        when(outboxEventRepository.findUnsentBatch()).thenReturn(List.of());

        outboxPoller.publishOutbox();
        verify(producer, never()).publishTradeCreated(any(), any());
    }

    @Test
    void publishOutbox_ShouldPublishWithNullCorrelationId_whenHeadersAreNull() throws JsonProcessingException {
        OutboxEvent noHeaders = getOutboxEvent("TRD-003", null);
        when(outboxEventRepository.findUnsentBatch()).thenReturn(List.of(noHeaders));

        outboxPoller.publishOutbox();

        ArgumentCaptor<TradeCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TradeCreatedEvent.class);
        ArgumentCaptor<String> cidCaptor = ArgumentCaptor.forClass(String.class);
        verify(producer).publishTradeCreated(eventCaptor.capture(), cidCaptor.capture());
        assertNull(cidCaptor.getValue());
        assertEquals("TRD-003", eventCaptor.getValue().getTradeId());
        assertNotNull(noHeaders.getSentAt());
    }

    @Test
    void publishOutbox_ShouldNotMarkSent_whenPublishFails() throws JsonProcessingException {
        OutboxEvent row = getOutboxEvent("TRD-004", "{\"correlationId\":\"cid-4\"}");
        when(outboxEventRepository.findUnsentBatch()).thenReturn(List.of(row));
        doThrow(new TradePublishException("kafka down", new RuntimeException()))
                .when(producer).publishTradeCreated(any(), any());

        assertThrows(TradePublishException.class, () -> outboxPoller.publishOutbox());
        assertNull(row.getSentAt());
        verify(producer, times(1))
                .publishTradeCreated(any(TradeCreatedEvent.class), eq("cid-4"));
    }

    @Test
    void publishOutbox_ShouldThrowAndNotPublish_whenPayloadIsInvalid() throws JsonProcessingException {
        OutboxEvent row = getOutboxEvent("TRD-005", "{\"correlationId\":\"cid-5\"}");
        row.setPayload("not json");
        when(outboxEventRepository.findUnsentBatch()).thenReturn(List.of(row));

        assertThrows(JsonProcessingException.class, () -> outboxPoller.publishOutbox());
        verify(producer, never())
                .publishTradeCreated(any(TradeCreatedEvent.class), anyString());
    }

}
