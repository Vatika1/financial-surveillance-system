package com.financialsurveillance.tradingestion.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.domain.OutboxEvent;
import com.financialsurveillance.tradingestion.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TradeEventProducer producer;

    @Scheduled(fixedDelay = 100)
    @Transactional
    public void publishOutbox() throws JsonProcessingException {

        List<OutboxEvent> unsentRows = outboxEventRepository.findUnsentBatch();
        if (unsentRows.isEmpty()) {
            return;
        }

        for(OutboxEvent row: unsentRows){
            TradeCreatedEvent event = objectMapper.readValue(row.getPayload(), TradeCreatedEvent.class);
            String correlationId = row.getHeaders() == null ? null
                    : objectMapper.readTree(row.getHeaders()).path("correlationId").asText(null);
            producer.publishTradeCreated(event, correlationId);
            row.setSentAt(ZonedDateTime.now());
        }
    }
}