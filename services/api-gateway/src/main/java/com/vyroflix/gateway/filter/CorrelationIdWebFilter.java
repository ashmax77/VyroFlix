package com.vyroflix.gateway.filter;

import com.vyroflix.common.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFlux filter that runs at the very highest precedence across ALL requests
 * (including actuator endpoints, security rejections, and routed microservices).
 *
 * <p>Ensures that every request entering the platform has an {@code X-Correlation-Id}
 * header, propagates it downstream to routed microservices, and guarantees that it
 * is present in every HTTP response to the client.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String incomingCorrelationId = request.getHeaders().getFirst(Constants.HEADER_CORRELATION_ID);

        final String correlationId;
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            correlationId = incomingCorrelationId;
            log.debug("Preserving incoming correlation ID: {}", correlationId);
        }

        // Ensure the correlation ID header is written to the response
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(Constants.HEADER_CORRELATION_ID, correlationId);
            return Mono.empty();
        });
        exchange.getResponse().getHeaders().set(Constants.HEADER_CORRELATION_ID, correlationId);

        // Forward correlation ID downstream to microservices
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(Constants.HEADER_CORRELATION_ID, correlationId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
