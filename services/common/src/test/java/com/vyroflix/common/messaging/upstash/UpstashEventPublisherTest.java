package com.vyroflix.common.messaging.upstash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UpstashEventPublisherTest {

    private MockRestServiceServer mockServer;
    private UpstashEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RestClient.Builder builder = RestClient.builder().baseUrl("https://example-kafka.upstash.io");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        publisher = new UpstashEventPublisher(restClient, objectMapper);
    }

    @Test
    @DisplayName("publishAsync sends POST request to Upstash REST API")
    void publishAsyncSendsPost() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                UUID.randomUUID(),
                Map.of("key", "val")
        );

        mockServer.expect(requestTo("https://example-kafka.upstash.io/produce/test-topic/" + event.getEventId()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"topic\":\"test-topic\",\"partition\":0,\"offset\":1}", MediaType.APPLICATION_JSON));

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", event);
        assertDoesNotThrow(future::join);
        mockServer.verify();
    }

    @Test
    @DisplayName("publishAsync with custom key sends to topic/key URL")
    void publishAsyncWithCustomKey() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                UUID.randomUUID(),
                Map.of("key", "val")
        );

        mockServer.expect(requestTo("https://example-kafka.upstash.io/produce/test-topic/custom-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"topic\":\"test-topic\",\"partition\":0,\"offset\":2}", MediaType.APPLICATION_JSON));

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", "custom-key", event);
        assertDoesNotThrow(future::join);
        mockServer.verify();
    }

    @Test
    @DisplayName("publishAsync fails on Upstash 500 server error")
    void publishAsyncServerError() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                UUID.randomUUID(),
                Map.of("key", "val")
        );

        mockServer.expect(requestTo("https://example-kafka.upstash.io/produce/test-topic/" + event.getEventId()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", event);
        assertThrows(Exception.class, future::join);
        mockServer.verify();
    }
}
