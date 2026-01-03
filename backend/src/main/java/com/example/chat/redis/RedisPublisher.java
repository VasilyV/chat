package com.example.chat.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class RedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final Executor executor =
            Executors.newFixedThreadPool(4);

    public RedisPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String payload) {
        executor.execute(() -> {
            try {
                redisTemplate.convertAndSend(channel, payload);
                log.debug("Redis published to channel={} bytes={}", channel, payload.length());
            } catch (Exception e) {
                log.error("Redis publish failed to channel={}", channel, e);
            }
        });
    }
}