package com.vyroflix.gateway.filter;

import com.vyroflix.common.security.SupabaseJwtConverter;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.common.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that extracts authenticated user context from the validated JWT
 * and propagates {@code X-User-Id} and {@code X-User-Role} headers downstream.
 */
@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    UserPrincipal principal = SupabaseJwtConverter.extractPrincipal(jwt);

                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
                    if (principal.getUserId() != null) {
                        requestBuilder.header(Constants.HEADER_USER_ID, principal.getUserId().toString());
                    }
                    if (principal.getRole() != null) {
                        requestBuilder.header(Constants.HEADER_USER_ROLE, principal.getRole());
                    }

                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    @Override
    public int getOrder() {
        // Run after correlation ID filter and security authentication
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
