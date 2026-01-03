package com.example.chat.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - start) / 1_000_000;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = isUserAuthenticated(auth) ? auth.getName() : "-";

            log.info("{} {} -> {} ({} ms) user={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    tookMs,
                    user
            );

            MDC.remove("traceId");
        }
    }

    private boolean isUserAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getName() != null;
    }
}


