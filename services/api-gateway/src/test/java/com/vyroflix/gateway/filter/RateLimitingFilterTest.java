package com.vyroflix.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitingFilterTest {

    @Test
    @DisplayName("RateLimitingFilter permits requests below threshold")
    void permitsRequestsBelowLimit() {
        RateLimitingFilter filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "maxRequestsPerMinute", 5);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/search").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("RateLimitingFilter returns HTTP 429 when threshold exceeded")
    void rejectsRequestsExceedingLimit() {
        RateLimitingFilter filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "maxRequestsPerMinute", 2);

        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // First 2 requests succeed
        MockServerHttpRequest req1 = MockServerHttpRequest.get("/api/v1/search").header("X-User-Id", "u123").build();
        filter.filter(MockServerWebExchange.from(req1), chain).block();

        MockServerHttpRequest req2 = MockServerHttpRequest.get("/api/v1/search").header("X-User-Id", "u123").build();
        filter.filter(MockServerWebExchange.from(req2), chain).block();

        // 3rd request gets rate-limited (HTTP 429)
        MockServerHttpRequest req3 = MockServerHttpRequest.get("/api/v1/search").header("X-User-Id", "u123").build();
        MockServerWebExchange exchange3 = MockServerWebExchange.from(req3);

        StepVerifier.create(filter.filter(exchange3, chain))
                .verifyComplete();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange3.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }
}
