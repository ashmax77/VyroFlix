package com.vyroflix.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.error.ErrorResponse;
import com.vyroflix.common.security.SupabaseJwtConverter;
import com.vyroflix.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Reactive security configuration for the API Gateway.
 *
 * <p>
 * Validates Supabase-issued JWTs against the configured JWKS endpoint.
 * Public routes (health, auth, catalog browsing, search) are accessible without
 * authentication.
 * User and Admin routes require valid bearer tokens.
 * </p>
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        // Actuator health & info
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Public auth endpoints (Supabase handles login/register)
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // Public catalog browsing and search (GET only)
                        .pathMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()
                        // Admin-only endpoints
                        .pathMatchers("/api/v1/admin/**").hasRole(Constants.ROLE_ADMIN)
                        .pathMatchers(HttpMethod.POST, "/api/v1/videos/upload-intents").hasRole(Constants.ROLE_ADMIN)
                        // All other /api/v1/** endpoints require authentication
                        .pathMatchers("/api/v1/**").authenticated()
                        // Any remaining requests
                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(new SupabaseJwtConverter())))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .build();
    }

    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            log.warn("Unauthorized access attempt: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath());
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "Authentication required");
        };
    }

    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> {
            log.warn("Forbidden access attempt: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath());
            return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
        };
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            ErrorCode errorCode,
            String message) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest().getHeaders().getFirst(Constants.HEADER_CORRELATION_ID);

        ErrorResponse errorBody = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(errorCode.name())
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .correlationId(correlationId)
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"" + errorCode.name() + "\",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
