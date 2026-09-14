package com.vyroflix.common.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.error.ApiException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("publishAsync publishes event using eventId as default partition key")
    void publishAsyncWithDefaultKey() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                UUID.randomUUID(),
                Map.of("key", "val")
        );

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(new ProducerRecord<>("test-topic", event.getEventId().toString(), "{}"), metadata);
        when(kafkaTemplate.send(eq("test-topic"), eq(event.getEventId().toString()), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", event);
        assertNotNull(future);
        assertDoesNotThrow(future::join);

        verify(kafkaTemplate).send(eq("test-topic"), eq(event.getEventId().toString()), any(String.class));
    }

    @Test
    @DisplayName("publishAsync with custom partition key sends with that key")
    void publishAsyncWithCustomKey() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "catalog.title.published.v1",
                "content-service",
                UUID.randomUUID(),
                Map.of("titleId", "movie-123")
        );

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(new ProducerRecord<>("test-topic", "movie-123", "{}"), metadata);
        when(kafkaTemplate.send(eq("test-topic"), eq("movie-123"), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", "movie-123", event);
        assertDoesNotThrow(future::join);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("test-topic"), eq("movie-123"), payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().contains("catalog.title.published.v1"));
        assertTrue(payloadCaptor.getValue().contains("movie-123"));
    }

    @Test
    @DisplayName("publishAsync handles send failure by wrapping in ApiException")
    void publishAsyncFailure() {
        EventEnvelope<Map<String, String>> event = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                UUID.randomUUID(),
                Map.of("key", "val")
        );

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        CompletableFuture<Void> future = publisher.publishAsync("test-topic", event);
        Exception ex = assertThrows(Exception.class, future::join);
        assertTrue(ex.getCause() instanceof ApiException || ex instanceof ApiException);
    }
}
