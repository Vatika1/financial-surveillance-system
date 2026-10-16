package com.financialsurveillance.tradingestion.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.domain.OutboxEvent;
import com.financialsurveillance.tradingestion.exception.TradePublishException;
import com.financialsurveillance.tradingestion.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        List<CompletableFuture<?>> futures = new ArrayList<>(unsentRows.size());
        for (OutboxEvent row : unsentRows) {
            TradeCreatedEvent event = objectMapper.readValue(row.getPayload(), TradeCreatedEvent.class);
            String correlationId = row.getHeaders() == null ? null
                    : objectMapper.readTree(row.getHeaders()).path("correlationId").asText(null);
            futures.add(producer.publishTradeCreatedAsync(event, correlationId));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TradePublishException("Interrupted while publishing outbox batch", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new TradePublishException("Outbox batch publish failed", e);
        }

        ZonedDateTime now = ZonedDateTime.now();
        unsentRows.forEach(row -> row.setSentAt(now));
    }
}