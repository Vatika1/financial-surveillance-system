package com.financialsurveillance.tradingestion.service;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.domain.Trade;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.mapper.TradeMapper;
import com.financialsurveillance.tradingestion.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class TradeIngestionService {

    private final KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate;
    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;
    @Value("${kafka.topics.trades-raw}")
    private String topic;
    private static final Set<String> symbols = new HashSet<>(Set.of(
            "AAPL", "MSFT", "JPM", "GS", "TSLA",
            "AMZN", "GOOGL", "META", "BAC", "WFC"
    ));

    public TradeIngestionService(KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate, TradeRepository tradeRepository, TradeMapper tradeMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.tradeRepository = tradeRepository;
        this.tradeMapper = tradeMapper;
    }

    @Transactional
    public TradeResponse processTrade(TradeRequest request){

        log.info("Processing trade tradeId={} advisorId={}", request.getTradeId(), request.getAdvisorId());
        validateBusinessRules(request);
        Trade trade = tradeMapper.toEntity(request);
        Trade savedTrade = tradeRepository.save(trade);
        log.info("Trade persisted successfully tradeId={} advisorId={}", request.getTradeId(), request.getAdvisorId());
        TradeCreatedEvent tradeCreatedEvent = tradeMapper.toTradeCreatedEvent(savedTrade);
        kafkaTemplate.send(topic,  savedTrade.getAdvisorId(), tradeCreatedEvent);
        log.info("Event published to Kafka successfully tradeId={} advisorId={}", request.getTradeId(), request.getAdvisorId());

        return tradeMapper.toResponseDto(savedTrade);
    }

    private void validateBusinessRules(TradeRequest request){

        if(tradeRepository.existsByTradeId(request.getTradeId())){
            log.warn("Duplicate trade tradeId={} advisorId={}", request.getTradeId(), request.getAdvisorId());
            throw new DuplicateTradeException(request.getTradeId());
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(3);
        if(request.getTradeTimestamp().isBefore(cutoff)){
            log.warn("Old Trade timestamp tradeTimestamp{} tradeId={} advisorId={}", request.getTradeTimestamp(), request.getTradeId(), request.getAdvisorId());
            throw new InvalidTradeException(request.getTradeId(), "Trade timestamp is older than the allowed processing window");
        }
        if(!symbols.contains(request.getSymbol())){
            log.warn("Invalid trade symbol symbol={} tradeId={} advisorId={}", request.getSymbol(), request.getTradeId(), request.getAdvisorId());
            throw new InvalidTradeException(request.getTradeId(), "Invalid trade symbol");
        }
    }
}
