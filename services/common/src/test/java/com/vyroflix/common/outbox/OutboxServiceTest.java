package com.vyroflix.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        outboxService = new OutboxService(outboxRepository, objectMapper);
    }

    @Test
    @DisplayName("save persists an EventEnvelope as an OutboxEvent with PENDING status")
    void saveEnvelope() {
        UUID correlationId = UUID.randomUUID();
        EventEnvelope<Map<String, String>> envelope = EventEnvelope.of(
                "video.uploaded.v1",
                "video-service",
                correlationId,
                Map.of("rawKey", "raw/test.mp4")
        );

        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent saved = outboxService.save(envelope);

        assertNotNull(saved);
        assertEquals(envelope.getEventId(), saved.getEventId());
        assertEquals("video.uploaded.v1", saved.getEventType());
        assertEquals("video-service", saved.getProducer());
        assertEquals(correlationId, saved.getCorrelationId());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertTrue(saved.getPayload().contains("raw/test.mp4"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("video.uploaded.v1", captor.getValue().getEventType());
    }

    @Test
    @DisplayName("markPublished updates outbox event status and publishedAt timestamp")
    void markPublished() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(id)
                .eventId(UUID.randomUUID())
                .eventType("video.uploaded.v1")
                .producer("video-service")
                .correlationId(UUID.randomUUID())
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();

        when(outboxRepository.findById(id)).thenReturn(Optional.of(event));

        outboxService.markPublished(id);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("handleFailure increments retry count and marks FAILED when threshold reached")
    void handleFailureExceedingMaxRetries() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(id)
                .eventId(UUID.randomUUID())
                .eventType("video.uploaded.v1")
                .producer("video-service")
                .correlationId(UUID.randomUUID())
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(2)
                .build();

        when(outboxRepository.findById(id)).thenReturn(Optional.of(event));

        outboxService.handleFailure(id, 3, new RuntimeException("Broker connection error"));

        assertEquals(3, event.getRetryCount());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("cleanupPublished deletes published records older than cutoff")
    void cleanupPublished() {
        Instant cutoff = Instant.now();
        when(outboxRepository.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, cutoff)).thenReturn(5);

        int count = outboxService.cleanupPublished(cutoff);

        assertEquals(5, count);
        verify(outboxRepository).deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, cutoff);
    }
}
