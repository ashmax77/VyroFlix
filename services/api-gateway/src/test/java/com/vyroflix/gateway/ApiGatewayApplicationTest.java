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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
