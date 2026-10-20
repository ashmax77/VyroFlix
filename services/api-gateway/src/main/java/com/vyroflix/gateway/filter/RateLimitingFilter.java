package com.vyroflix.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-bucket rate limiting filter for the public API Gateway.
 * Enforces per-client request limits to protect downstream microservices from overload.
 */
@Slf4j
@Component
public class RateLimitingFilter implements WebFilter, Ordered {

    @Value("${vyroflix.gateway.rate-limit.requests-per-minute:200}")
    private int maxRequestsPerMinute;

    private final Map<String, ClientRateTracker> clientBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String clientKey = resolveClientKey(exchange);
        ClientRateTracker tracker = clientBuckets.computeIfAbsent(clientKey, k -> new ClientRateTracker());

        if (!tracker.tryAcquire(maxRequestsPerMinute)) {
            log.warn("Rate limit exceeded for client [{}]: {} requests in current window",
                    clientKey, maxRequestsPerMinute);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add(HttpHeaders.RETRY_AFTER, "60");
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

            Map<String, Object> problem = Map.of(
                    "type", "https://api.vyroflix.com/errors/rate-limit-exceeded",
                    "title", "Too Many Requests",
                    "status", 429,
                    "detail", "Rate limit exceeded. Maximum " + maxRequestsPerMinute + " requests per minute allowed.",
                    "instance", exchange.getRequest().getPath().value()
            );

            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsString(problem).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException e) {
                bytes = "{\"status\":429,\"title\":\"Too Many Requests\"}".getBytes(StandardCharsets.UTF_8);
            }

            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(Constants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }

        if (exchange.getRequest().getRemoteAddress() != null) {
            return "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        // Run immediately after CorrelationIdWebFilter (-100)
        return -90;
    }

    private static class ClientRateTracker {
        private volatile long windowStart = Instant.now().getEpochSecond();
        private final AtomicInteger counter = new AtomicInteger(0);

        public synchronized boolean tryAcquire(int limit) {
            long currentSecond = Instant.now().getEpochSecond();
            if (currentSecond - windowStart >= 60) {
                windowStart = currentSecond;
                counter.set(0);
            }

            return counter.incrementAndGet() <= limit;
        }
    }
}
