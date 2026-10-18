package com.financialsurveillance.tradingestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class IdempotencyKeyService {

    private final StringRedisTemplate redisTemplate;
    private static final String PROCESSING = "PROCESSING";


    public boolean tryAcquire(String key){
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                        .setIfAbsent(redisKey(key), "PROCESSING", Duration.ofSeconds(60))
        );
    }

    public Optional<String> findCompletedResponse(String key){
        String response = redisTemplate.opsForValue()
                .get(redisKey(key));

        return Optional.ofNullable(response)
                .filter(v -> !PROCESSING.equals(v));
    }
    public void markCompleted(String key, String responseJson){
        redisTemplate.opsForValue().set(
                redisKey(key),
                responseJson,
                Duration.ofHours(24)
        );
    }
    public void release(String key){
        redisTemplate.opsForValue().getAndDelete(redisKey(key));
    }

    private String redisKey(String idempotencyKey) {
        return "idem:trades:" + idempotencyKey;
    }
}
