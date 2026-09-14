package com.vyroflix.common.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * Standard Apache Kafka event publisher using {@link KafkaTemplate}.
 *
 * <p>Serializes {@link EventEnvelope} instances to JSON strings via Jackson
 * to prevent cross-service type-header deserialization issues.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<Void> publishAsync(String topic, EventEnvelope<?> event) {
        String key = event.getEventId() != null ? event.getEventId().toString() : null;
        return publishAsync(topic, key, event);
    }

    @Override
    public CompletableFuture<Void> publishAsync(String topic, String key, EventEnvelope<?> event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.debug("Publishing event [{}] type [{}] to topic [{}] with key [{}]",
                    event.getEventId(), event.getEventType(), topic, key);

            return kafkaTemplate.send(topic, key, jsonPayload)
                    .thenAccept(result -> log.debug("Successfully published event [{}] to topic [{}] at partition [{}] offset [{}]",
                            event.getEventId(), topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()))
                    .exceptionally(ex -> {
                        log.error("Failed to publish event [{}] to topic [{}]: {}",
                                event.getEventId(), topic, ex.getMessage(), ex);
                        throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to publish Kafka event: " + ex.getMessage(), ex);
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event [{}] to JSON: {}", event.getEventId(), e.getMessage(), e);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to serialize event: " + e.getMessage(), e));
            return failed;
        }
    }
}
