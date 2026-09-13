package com.vyroflix.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard event envelope for all Kafka events in VyroFlix.
 *
 * <p>Every event published through the system must be wrapped in this envelope.
 * Consumers use {@code eventId} for idempotency and {@code correlationId}
 * for distributed tracing across the pipeline.</p>
 *
 * <p>Envelope format (from ARCHITECTURE.md):</p>
 * <pre>{@code
 * {
 *   "eventId": "uuid",
 *   "eventType": "video.uploaded.v1",
 *   "occurredAt": "2026-09-13T04:50:00Z",
 *   "producer": "video-service",
 *   "correlationId": "uuid",
 *   "payload": {}
 * }
 * }</pre>
 *
 * @param <T> the payload type
 */
@Getter
@ToString
@Builder
public class EventEnvelope<T> {

    /** Unique identifier for this event instance; used for consumer idempotency. */
    private final UUID eventId;

    /** Versioned event type, e.g. {@code "video.uploaded.v1"}. */
    private final String eventType;

    /** Timestamp when the event occurred (UTC). */
    private final Instant occurredAt;

    /** Name of the producing service, e.g. {@code "video-service"}. */
    private final String producer;

    /** Correlation ID for distributed tracing across the pipeline. */
    private final UUID correlationId;

    /** The domain-specific event payload. */
    private final T payload;

    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("producer") String producer,
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("payload") T payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.producer = producer;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    /**
     * Factory method to create a new envelope with auto-generated eventId and timestamp.
     *
     * @param eventType     versioned event type
     * @param producer      producing service name
     * @param correlationId correlation ID for tracing
     * @param payload       domain payload
     * @param <T>           payload type
     * @return a fully populated envelope
     */
    public static <T> EventEnvelope<T> of(
            String eventType,
            String producer,
            UUID correlationId,
            T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .occurredAt(Instant.now())
                .producer(producer)
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
