package com.vyroflix.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.error.ErrorResponse;
import com.vyroflix.common.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;

/**
 * Spring Security auto-configuration for servlet-based downstream microservices.
 *
 * <p>Validates Supabase-issued JWTs, extracts {@link UserPrincipal}, maps RBAC roles,
 * and standardizes error responses according to RFC 7807 problem details.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(prefix = "vyroflix.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(SupabaseJwtConverter.class)
    public SupabaseJwtConverter supabaseJwtConverter() {
        return new SupabaseJwtConverter();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            log.warn("Unauthorized access attempt on [{}] {}: {}",
                    request.getMethod(), request.getRequestURI(), authException.getMessage());
            writeErrorResponse(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "Authentication required", objectMapper);
        };
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> {
            log.warn("Forbidden access attempt on [{}] {}: {}",
                    request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
            writeErrorResponse(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Access denied", objectMapper);
        };
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            SupabaseJwtConverter converter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            if (properties.getPublicEndpoints() != null) {
                for (String endpoint : properties.getPublicEndpoints()) {
                    auth.requestMatchers(endpoint).permitAll();
                }
            }
            auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
            auth.requestMatchers("/api/v1/auth/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll();
            auth.requestMatchers("/api/v1/admin/**").hasRole(Constants.ROLE_ADMIN);
            auth.requestMatchers(HttpMethod.POST, "/api/v1/videos/upload-intents").hasRole(Constants.ROLE_ADMIN);
            auth.requestMatchers("/api/v1/**").authenticated();
            auth.anyRequest().permitAll();
        });

        if (jwtDecoderProvider.getIfAvailable() != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
        }

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    private void writeErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            ObjectMapper objectMapper) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String correlationId = request.getHeader(Constants.HEADER_CORRELATION_ID);

        ErrorResponse errorBody = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
