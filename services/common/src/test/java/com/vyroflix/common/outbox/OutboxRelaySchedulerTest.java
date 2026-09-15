package com.vyroflix.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private EventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private OutboxProperties properties;
    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        properties = new OutboxProperties();
        properties.setBatchSize(10);
        properties.setMaxRetries(3);
        properties.setRetentionDays(7);

        scheduler = new OutboxRelayScheduler(outboxService, eventPublisher, objectMapper, properties);
    }

    @Test
    @DisplayName("relayPendingEvents publishes pending events and marks them as PUBLISHED")
    void relayPendingEventsSuccess() {
        UUID eventId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.builder()
                .id(outboxId)
                .eventId(eventId)
                .eventType("video.uploaded.v1")
                .producer("video-service")
                .correlationId(correlationId)
                .payload("{\"key\":\"value\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(outboxService.findPendingEvents(10)).thenReturn(List.of(event));

        scheduler.relayPendingEvents();

        ArgumentCaptor<EventEnvelope<?>> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(eq("video.uploaded.v1"), eq(eventId.toString()), envelopeCaptor.capture());
        assertEquals(eventId, envelopeCaptor.getValue().getEventId());
        assertEquals("video.uploaded.v1", envelopeCaptor.getValue().getEventType());
        assertEquals(correlationId, envelopeCaptor.getValue().getCorrelationId());

        verify(outboxService).markPublished(outboxId);
    }

    @Test
    @DisplayName("relayPendingEvents records failure when publishing throws an exception")
    void relayPendingEventsFailure() {
        UUID outboxId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.builder()
                .id(outboxId)
                .eventId(eventId)
                .eventType("video.uploaded.v1")
                .producer("video-service")
                .correlationId(UUID.randomUUID())
                .payload("{\"key\":\"value\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(outboxService.findPendingEvents(10)).thenReturn(List.of(event));
        doThrow(new RuntimeException("Kafka unreachable"))
                .when(eventPublisher).publish(any(), any(), any());

        scheduler.relayPendingEvents();

        verify(outboxService).handleFailure(eq(outboxId), eq(3), any(RuntimeException.class));
        verify(outboxService, never()).markPublished(any());
    }

    @Test
    @DisplayName("cleanupOldEvents triggers outbox service cleanup with retention cutoff")
    void cleanupOldEvents() {
        scheduler.cleanupOldEvents();
        verify(outboxService).cleanupPublished(any(Instant.class));
    }
}
