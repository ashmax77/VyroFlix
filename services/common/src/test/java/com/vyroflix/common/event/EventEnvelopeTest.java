package com.vyroflix.common.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventEnvelopeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("of() factory generates eventId and occurredAt automatically")
    void factoryPopulatesIdAndTimestamp() {
        UUID correlationId = UUID.randomUUID();
        Map<String, String> payload = Map.of("key", "value");

        EventEnvelope<Map<String, String>> envelope = EventEnvelope.of(
                "test.event.v1",
                "test-service",
                correlationId,
                payload);

        assertNotNull(envelope.getEventId(), "eventId must be auto-generated");
        assertEquals("test.event.v1", envelope.getEventType());
        assertEquals("test-service", envelope.getProducer());
        assertEquals(correlationId, envelope.getCorrelationId());
        assertNotNull(envelope.getOccurredAt(), "occurredAt must be auto-generated");
        assertEquals(payload, envelope.getPayload());
    }

    @Test
    @DisplayName("Serializes and deserializes to/from JSON correctly")
    void jsonRoundTrip() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-13T04:50:00Z");

        EventEnvelope<Map<String, String>> original = EventEnvelope.<Map<String, String>>builder()
                .eventId(eventId)
                .eventType("video.uploaded.v1")
                .occurredAt(now)
                .producer("video-service")
                .correlationId(correlationId)
                .payload(Map.of("uploadIntentId", "abc-123"))
                .build();

        String json = mapper.writeValueAsString(original);

        // Verify JSON structure
        assertTrue(json.contains("\"eventId\""));
        assertTrue(json.contains("\"eventType\":\"video.uploaded.v1\""));
        assertTrue(json.contains("\"producer\":\"video-service\""));
        assertTrue(json.contains("\"occurredAt\":\"2026-09-13T04:50:00Z\""));

        // Deserialize
        EventEnvelope<Map<String, String>> deserialized = mapper.readValue(
                json, new TypeReference<>() {});

        assertEquals(eventId, deserialized.getEventId());
        assertEquals("video.uploaded.v1", deserialized.getEventType());
        assertEquals("video-service", deserialized.getProducer());
        assertEquals(correlationId, deserialized.getCorrelationId());
        assertEquals(now, deserialized.getOccurredAt());
        assertEquals("abc-123", deserialized.getPayload().get("uploadIntentId"));
    }
}
