package com.vyroflix.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Event payload emitted when a title transitions to PUBLISHED.
 * Consumed by search-service (to index the title) and recommendation-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitlePublishedEventPayload {
    private UUID titleId;
    private String title;
    private String type; // MOVIE, SERIES
    private String synopsis;
    private Set<String> genreSlugs;
    private Integer releaseYear;
    private String maturityRating;
    private Integer durationMinutes;
    private String posterUrl;
    private String backdropUrl;
    /** Cast (ACTOR) names. Optional: absent in events from older producers. */
    private List<String> castMembers;
    /** Director names. Optional: absent in events from older producers. */
    private List<String> directors;
    private Instant publishedAt;
}
