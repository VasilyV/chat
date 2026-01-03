package com.example.chat.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static String base64Secret32Bytes() {
        byte[] raw = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(raw);
    }

    @Test
    void createValidateAndParseUsername_shouldWork() {
        JwtTokenProvider provider = new JwtTokenProvider(base64Secret32Bytes(), 60_000);

        String token = provider.createAccessToken("alice", List.of("ROLE_USER"));

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void validateToken_shouldReturnFalse_forMalformedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(base64Secret32Bytes(), 60_000);

        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forExpiredToken() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(base64Secret32Bytes(), 1);

        String token = provider.createAccessToken("alice", List.of("ROLE_USER"));
        Thread.sleep(5);

        assertThat(provider.validateToken(token)).isFalse();
    }
}
