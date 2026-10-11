package com.financialsurveillance.activitymonitor.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisTradeWindowStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private RedisTradeWindowStore store;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        store = new RedisTradeWindowStore(redisTemplate, objectMapper);
    }

    private TradeCreatedEvent getTradeCreatedEvent(){
        return TradeCreatedEvent.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    void addTrade_writesToSortedSetWithTimestampScore() throws JsonProcessingException {
        // arrange
        TradeCreatedEvent trade = getTradeCreatedEvent();

        //act
        store.addTrade("ADV-001", trade);

        // assert
        String expectedJson = objectMapper.writeValueAsString(trade);
        long expectedScore = trade.getTradeTimestamp().toInstant().toEpochMilli();
        verify(zSetOps).add("window:ADV-001", expectedJson, (double) expectedScore);
    }
}
