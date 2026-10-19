package com.vyroflix.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponse {
    private UUID id;
    private UUID titleId;
    private int seasonNumber;
    private String name;
    private String overview;
    private List<EpisodeResponse> episodes;
    private Instant createdAt;
    private Instant updatedAt;
}
