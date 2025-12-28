package com.example.chat.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class RedisPublisher {

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
            } catch (Exception e) {

            }
        });
    }
}