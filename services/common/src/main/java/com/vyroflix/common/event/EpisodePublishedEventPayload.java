package com.vyroflix.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an episode becomes PUBLISHED as part of publishing its series.
 * Consumed by streaming-service so playback can be authorised per episode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodePublishedEventPayload {
    private UUID episodeId;
    private UUID titleId;
    private Instant publishedAt;
}
