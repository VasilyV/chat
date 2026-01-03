package com.example.chat.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider tokenProvider;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_shouldMatchPublicPaths() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/login");
        assertThat(filter.shouldNotFilter(req)).isTrue();

        req.setRequestURI("/api/auth/logout");
        assertThat(filter.shouldNotFilter(req)).isTrue();

        req.setRequestURI("/api/auth/refresh");
        assertThat(filter.shouldNotFilter(req)).isTrue();

        req.setRequestURI("/ws-chat");
        assertThat(filter.shouldNotFilter(req)).isTrue();

        req.setRequestURI("/api/chat/rooms/1/messages");
        assertThat(filter.shouldNotFilter(req)).isFalse();
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenBearerTokenValid() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/me");
        req.addHeader("Authorization", "Bearer TOKEN");

        when(tokenProvider.validateToken("TOKEN")).thenReturn(true);
        when(tokenProvider.getUsername("TOKEN")).thenReturn("alice");

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_shouldUseCookie_whenNoAuthorizationHeader() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/me");
        req.setCookies(new Cookie("accessToken", "COOKIE_TOKEN"));

        when(tokenProvider.validateToken("COOKIE_TOKEN")).thenReturn(true);
        when(tokenProvider.getUsername("COOKIE_TOKEN")).thenReturn("bob");

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("bob");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_shouldClearAuthentication_whenTokenInvalid() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("old", null)
        );

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/me");
        req.addHeader("Authorization", "Bearer BAD");

        when(tokenProvider.validateToken("BAD")).thenReturn(false);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }
}
