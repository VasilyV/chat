package com.example.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isAllowed_whenFirstRequest_shouldSetExpiryAndReturnTrue() {
        String key = "user1";
        String fullKey = "rate_limit:user1";
        when(valueOperations.increment(fullKey)).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed(key, 5, 10);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(eq(fullKey), eq(Duration.ofSeconds(10)));
    }

    @Test
    void isAllowed_whenUnderLimit_shouldNotSetExpiryAndReturnTrue() {
        String key = "user1";
        String fullKey = "rate_limit:user1";
        when(valueOperations.increment(fullKey)).thenReturn(3L);

        boolean allowed = rateLimiterService.isAllowed(key, 5, 10);

        assertThat(allowed).isTrue();
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }

    @Test
    void isAllowed_whenOverLimit_shouldReturnFalse() {
        String key = "user1";
        String fullKey = "rate_limit:user1";
        when(valueOperations.increment(fullKey)).thenReturn(6L);

        boolean allowed = rateLimiterService.isAllowed(key, 5, 10);

        assertThat(allowed).isFalse();
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }

    @Test
    void isAllowed_whenRedisReturnsNull_shouldReturnFalse() {
        String key = "user1";
        String fullKey = "rate_limit:user1";
        when(valueOperations.increment(fullKey)).thenReturn(null);

        boolean allowed = rateLimiterService.isAllowed(key, 5, 10);

        assertThat(allowed).isFalse();
    }
}
