package com.vyroflix.common.messaging.upstash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

/**
 * Upstash Kafka REST API event publisher.
 *
 * <p>Used in the cloud/production deployment where direct Kafka TCP connections
 * are replaced by Upstash's serverless HTTP REST API.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class UpstashEventPublisher implements EventPublisher {

    private final RestClient upstashRestClient;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<Void> publishAsync(String topic, EventEnvelope<?> event) {
        String key = event.getEventId() != null ? event.getEventId().toString() : null;
        return publishAsync(topic, key, event);
    }

    @Override
    public CompletableFuture<Void> publishAsync(String topic, String key, EventEnvelope<?> event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(event);
                String path = (key != null && !key.isBlank())
                        ? "/produce/" + topic + "/" + key
                        : "/produce/" + topic;

                log.debug("Publishing event [{}] to Upstash Kafka REST endpoint [{}]", event.getEventId(), path);

                upstashRestClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonPayload)
                        .retrieve()
                        .toBodilessEntity();

                log.debug("Successfully published event [{}] to Upstash topic [{}]", event.getEventId(), topic);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize event [{}] to JSON: {}", event.getEventId(), e.getMessage(), e);
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to serialize event: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Failed to publish event [{}] to Upstash REST API: {}", event.getEventId(), e.getMessage(), e);
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to publish Upstash Kafka event: " + e.getMessage(), e);
            }
        });
    }
}
