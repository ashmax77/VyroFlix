package com.vyroflix.gateway;

import com.vyroflix.common.util.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ApiGatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    @DisplayName("Health endpoint is publicly accessible and returns UP")
    void healthEndpointIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("CorrelationIdFilter generates X-Correlation-ID when missing")
    void generatesCorrelationIdWhenMissing() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(Constants.HEADER_CORRELATION_ID, val -> {
                    assertNotNull(val);
                    // Should be valid UUID
                    UUID.fromString(val);
                });
    }

    @Test
    @DisplayName("CorrelationIdFilter preserves incoming X-Correlation-ID")
    void preservesIncomingCorrelationId() {
        String clientCorrelationId = UUID.randomUUID().toString();

        webTestClient.get()
                .uri("/actuator/health")
                .header(Constants.HEADER_CORRELATION_ID, clientCorrelationId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(Constants.HEADER_CORRELATION_ID, clientCorrelationId);
    }

    @Test
    @DisplayName("Protected endpoint without token returns 401 with standard ErrorResponse")
    void protectedEndpointWithoutTokenReturns401() {
        webTestClient.post()
                .uri("/api/v1/streaming/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists(Constants.HEADER_CORRELATION_ID)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo("/api/v1/streaming/sessions");
    }

    @Test
    @DisplayName("Admin endpoint with regular USER token returns 403 Forbidden")
    void adminEndpointWithUserTokenReturns403() {
        Jwt userJwt = new Jwt(
                "mock-user-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of(
                        "sub", UUID.randomUUID().toString(),
                        "email", "user@vyroflix.local",
                        "role", "authenticated",
                        "aud", "authenticated"
                )
        );
        when(reactiveJwtDecoder.decode(anyString())).thenReturn(Mono.just(userJwt));

        webTestClient.post()
                .uri("/api/v1/admin/maintenance")
                .headers(h -> h.setBearerAuth("mock-user-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().exists(Constants.HEADER_CORRELATION_ID)
                .expectBody()
                .jsonPath("$.code").isEqualTo("FORBIDDEN")
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    @DisplayName("Admin endpoint with ADMIN token passes security authorization")
    void adminEndpointWithAdminTokenPassesSecurity() {
        Jwt adminJwt = new Jwt(
                "mock-admin-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of(
                        "sub", UUID.randomUUID().toString(),
                        "email", "admin@vyroflix.local",
                        "app_metadata", Map.of("role", "ADMIN"),
                        "aud", "authenticated"
                )
        );
        when(reactiveJwtDecoder.decode(anyString())).thenReturn(Mono.just(adminJwt));

        webTestClient.post()
                .uri("/api/v1/admin/maintenance")
                .headers(h -> h.setBearerAuth("mock-admin-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().value(status -> {
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status);
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status);
                });
    }

    @Test
    @DisplayName("Public catalog endpoint is accessible without authentication")
    void publicCatalogEndpointAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/catalog/titles")
                .exchange()
                .expectStatus().value(status -> {
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status);
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status);
                });
    }
}
