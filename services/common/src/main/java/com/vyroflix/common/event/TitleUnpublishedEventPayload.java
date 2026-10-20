package com.vyroflix.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a title transitions from PUBLISHED to UNPUBLISHED or is deleted.
 * Consumed by search-service (to evict or update state) and recommendation-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitleUnpublishedEventPayload {
    private UUID titleId;
    private Instant unpublishedAt;
    private String reason;
}
