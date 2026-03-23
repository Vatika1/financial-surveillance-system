package com.financialsurveillance.tradingestion.controller;

import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.service.TradeIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeIngestionService tradeService;

    public TradeController(TradeIngestionService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> submitTrade(@Valid @RequestBody TradeRequest request){
        TradeResponse tradeResponse = tradeService.processTrade(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeResponse);
    }
}
