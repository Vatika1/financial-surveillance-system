package com.financialsurveillance.tradingestion.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.service.IdempotencyKeyService;
import com.financialsurveillance.tradingestion.service.TradeIngestionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeIngestionService tradeService;
    private final Counter tradesIngested;
    private final IdempotencyKeyService idempotencyKeyService;
    private final ObjectMapper objectMapper;

    public TradeController(TradeIngestionService tradeService, MeterRegistry registry, IdempotencyKeyService idempotencyKeyService, ObjectMapper objectMapper) {
        this.tradeService = tradeService;
        this.tradesIngested = Counter.builder("trades_ingested_total")
                .description("Trades accepted by trade-ingestion")
                .register(registry);
        this.idempotencyKeyService = idempotencyKeyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> submitTrade(@Valid @RequestBody TradeRequest request,
                                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws JsonProcessingException {
        // No header: old behaviour
        if (idempotencyKey == null) {
            return created(tradeService.processTrade(request));   // old path
        }

        // Retry: key already exists
        boolean isNewRequest = idempotencyKeyService.tryAcquire(idempotencyKey);
        if(!isNewRequest){
            Optional<String> saved = idempotencyKeyService.findCompletedResponse(idempotencyKey);
            if (saved.isPresent()) {
                return created(objectMapper.readValue(saved.get(), TradeResponse.class));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // New request: save, then store the response
        try {
            TradeResponse tradeResponse = tradeService.processTrade(request);
            idempotencyKeyService.markCompleted(idempotencyKey,
                    objectMapper.writeValueAsString(tradeResponse));
            tradesIngested.increment();
            return created(tradeResponse);
        } catch (Exception e) {
            idempotencyKeyService.release(idempotencyKey);
            throw e;
        }
    }

    private ResponseEntity<TradeResponse> created(TradeResponse body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

}
