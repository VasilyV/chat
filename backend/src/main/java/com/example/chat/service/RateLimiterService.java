package com.example.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String fullKey = "rate_limit:" + key;
        Long current = redisTemplate.opsForValue().increment(fullKey);

        if (current != null && current == 1) {
            redisTemplate.expire(fullKey, Duration.ofSeconds(windowSeconds));
        }

        return current != null && current <= maxRequests;
    }
}
