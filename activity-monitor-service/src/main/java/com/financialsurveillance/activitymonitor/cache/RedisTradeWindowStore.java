package com.financialsurveillance.activitymonitor.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Primary
@Service
public class RedisTradeWindowStore implements TradeWindowStore{
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration MAX_WINDOW = Duration.ofMinutes(15);

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
        String key = "window:" + advisorId;
        double score = trade.getTradeTimestamp().toInstant().toEpochMilli();            // epoch millis
        redisTemplate.opsForZSet().add(key, json, score);
    }

    @Override
    public void removeTrade(String advisorId, TradeCreatedEvent trade) {
        String json;
        try {
            json = objectMapper.writeValueAsString(trade); // convert here
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize trade " + trade.getTradeId(), e);
        }
        String key = "window:" + advisorId;
        redisTemplate.opsForZSet().remove(key, json);
    }

    @Override
    public List<TradeCreatedEvent> getRecentTrades(String advisorId, Duration window) {
        String key = "window:" + advisorId;
        long now = System.currentTimeMillis();
        long from = now - window.toMillis();
        Set<String> recentTrades = redisTemplate.opsForZSet().rangeByScore(key, from, now);
        List<TradeCreatedEvent> recentTradesList = new ArrayList<>();

        for (String json : recentTrades) {
            TradeCreatedEvent trade = null;
            try {
                trade = objectMapper.readValue(json, TradeCreatedEvent.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not serialize trade " + json, e);
            }
            recentTradesList.add(trade);
        }
        System.out.println("getRecentTrades " + key + " -> " + recentTradesList.size());
        return recentTradesList;
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void cleanUpTrades() {
        Set <String> keys = redisTemplate.keys("window:*");
        long cutoff = System.currentTimeMillis() - MAX_WINDOW.toMillis();

        for(String key: keys){
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
        }
    }
}
