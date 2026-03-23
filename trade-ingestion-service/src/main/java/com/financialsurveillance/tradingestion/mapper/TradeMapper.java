package com.financialsurveillance.tradingestion.mapper;

import com.financialsurveillance.tradingestion.domain.Trade;
import com.financialsurveillance.tradingestion.domain.TradeStatus;
import com.financialsurveillance.tradingestion.domain.TradeType;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.event.TradeCreatedEvent;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class TradeMapper {

    public TradeResponse toResponseDto(Trade trade){
        return TradeResponse.builder()
                .id(trade.getId())
                .tradeId(trade.getTradeId())
                .createdAt(trade.getCreatedAt())
                .status(trade.getStatus())
                .build();
    }

    public Trade toEntity(TradeRequest request){
        return Trade.builder()
                .tradeId(request.getTradeId())
                .accountId(request.getAccountId())
                .price(request.getPrice())
                .advisorId(request.getAdvisorId())
                .clientId(request.getClientId())
                .symbol(request.getSymbol())
                .tradeTimestamp(request.getTradeTimestamp())
                .tradeType(request.getTradeType())
                .quantity(request.getQuantity())
                .sourceSystem(request.getSourceSystem())
                .sourceSystemId(request.getSourceSystemId())
                .currency(request.getCurrency())
                .exchange(request.getExchange())
                .createdAt(ZonedDateTime.now())
                .totalValue(request.getPrice().multiply(request.getQuantity()))
                .status(TradeStatus.RECEIVED)
                .build();
    }

    public TradeCreatedEvent toTradeCreatedEvent(Trade trade){
        return TradeCreatedEvent.builder()
                .accountId(trade.getAccountId())
                .id(trade.getId())
                .tradeTimestamp(trade.getTradeTimestamp())
                .advisorId(trade.getAdvisorId())
                .clientId(trade.getClientId())
                .createdAt(trade.getCreatedAt())
                .tradeType(trade.getTradeType())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .symbol(trade.getSymbol())
                .tradeId(trade.getTradeId())
                .sourceSystem(trade.getSourceSystem())
                .sourceSystemId(trade.getSourceSystemId())
                .totalValue(trade.getTotalValue())
                .quantity(trade.getQuantity())
                .currency(trade.getCurrency())
                .exchange(trade.getExchange())
                .build();
    }
}
