package com.financialsurveillance.tradingestion.service;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.domain.Trade;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.mapper.TradeMapper;
import com.financialsurveillance.tradingestion.producer.TradeEventProducer;
import com.financialsurveillance.tradingestion.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeIngestionService {

    private static final Set<String> SYMBOLS = new HashSet<>(Set.of(
            "AAPL", "MSFT", "JPM", "GS", "TSLA",
            "AMZN", "GOOGL", "META", "BAC", "WFC"
    ));

    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;
    private final TradeEventProducer tradeEventProducer;

    @Transactional
    public TradeResponse processTrade(TradeRequest request) {
        log.info("Processing trade tradeId={} advisorId={}",
                request.getTradeId(), request.getAdvisorId());

        validateBusinessRules(request);

        Trade savedTrade = tradeRepository.save(tradeMapper.toEntity(request));
        log.info("Trade persisted tradeId={} advisorId={}",
                savedTrade.getTradeId(), savedTrade.getAdvisorId());

        TradeCreatedEvent event = tradeMapper.toTradeCreatedEvent(savedTrade);
        tradeEventProducer.publishTradeCreated(event);

        return tradeMapper.toResponseDto(savedTrade);
    }

    private void validateBusinessRules(TradeRequest request) {
        if (tradeRepository.existsByTradeId(request.getTradeId())) {
            log.warn("Duplicate trade tradeId={} advisorId={}",
                    request.getTradeId(), request.getAdvisorId());
            throw new DuplicateTradeException(request.getTradeId());
        }
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(3);
        if (request.getTradeTimestamp().isBefore(cutoff)) {
            log.warn("Old trade timestamp tradeTimestamp={} tradeId={} advisorId={}",
                    request.getTradeTimestamp(), request.getTradeId(), request.getAdvisorId());
            throw new InvalidTradeException(request.getTradeId(),
                    "Trade timestamp is older than the allowed processing window");
        }
        if (!SYMBOLS.contains(request.getSymbol())) {
            log.warn("Invalid trade symbol symbol={} tradeId={} advisorId={}",
                    request.getSymbol(), request.getTradeId(), request.getAdvisorId());
            throw new InvalidTradeException(request.getTradeId(), "Invalid trade symbol");
        }
    }
}