package com.vyroflix.content.dto;

import com.vyroflix.content.entity.PublicationState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResponse {
    private UUID id;
    private UUID seasonId;
    private int episodeNumber;
    private String name;
    private String overview;
    private Integer durationSeconds;
    private String thumbnailUrl;
    private PublicationState publicationState;
    private boolean mediaReady;
    private Instant createdAt;
    private Instant updatedAt;
}
