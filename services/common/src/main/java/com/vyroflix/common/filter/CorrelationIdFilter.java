package com.vyroflix.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that propagates or generates a correlation ID for every request.
 *
 * <p>If the incoming request has an {@code X-Correlation-Id} header (set by the
 * API Gateway), that value is used. Otherwise a new UUID is generated.</p>
 *
 * <p>The correlation ID is placed into:</p>
 * <ul>
 *   <li>SLF4J MDC (key {@code correlationId}) — for structured log output</li>
 *   <li>Request attribute {@code correlationId} — for programmatic access</li>
 *   <li>Response header {@code X-Correlation-Id} — for client/debugging visibility</li>
 * </ul>
 *
 * <p>Per AGENTS.md: "Every external request must include or generate a
 * correlation ID."</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Set in MDC for structured logging
        MDC.put(MDC_KEY, correlationId);

        // Set as request attribute for GlobalExceptionHandler and controllers
        request.setAttribute(MDC_KEY, correlationId);

        // Echo back in response header
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
