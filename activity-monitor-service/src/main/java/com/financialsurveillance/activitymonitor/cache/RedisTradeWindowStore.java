package com.financialsurveillance.activitymonitor.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Primary
@Service
public class RedisTradeWindowStore implements TradeWindowStore{
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTradeWindowStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addTrade(String advisorId, TradeCreatedEvent trade){
        String json;
        try {
            json = objectMapper.writeValueAsString(trade); // convert here
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize trade " + trade.getTradeId(), e);
        }
        String key = "window:" + trade.getAdvisorId();
        double score = trade.getTradeTimestamp().toInstant().toEpochMilli();            // epoch millis
        redisTemplate.opsForZSet().add(key, json, score);
    }

    @Override
    public void removeTrade(String advisorId, TradeCreatedEvent trade) {

    }

    @Override
    public List<TradeCreatedEvent> getRecentTrades(String advisorId, Duration window) {
        return List.of();
    }

    @Override
    public void cleanUpTrades() {

    }
}
