package com.example.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        System.out.println("=== HEADERS ===");
        Collections.list(request.getHeaderNames()).forEach(h -> {
            System.out.println(h + " = " + request.getHeader(h));
        });
        System.out.println("================");

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (request.getCookies() != null) {
            System.out.println("Cookies received:");
            for (Cookie c : request.getCookies()) {
                System.out.println("Cookie: " + c.getName() + " = " + c.getValue());
            }
        } else {
            System.out.println("No cookies received at all.");
        }

        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    System.out.println("Found access token in cookie.");
                    break;
                }
            }
        }

        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsername(token);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("Authentication successful for " + username);
        } else {
            System.out.println("No valid token found.");
            SecurityContextHolder.clearContext();
        }

        System.out.println("Request here: " + request.getRequestURL());
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/refresh")
                || path.startsWith("/ws-chat");
    }
}

