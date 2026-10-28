package com.vyroflix.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Records that a consumer has already applied an event, keyed by the event envelope's {@code eventId}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "processed_events", schema = "recommendation")
@IdClass(ProcessedEvent.Key.class)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Id
    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String eventId;
        private String consumer;
    }
}
