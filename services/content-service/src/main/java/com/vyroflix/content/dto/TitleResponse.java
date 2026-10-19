package com.vyroflix.content.dto;

import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.TitleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitleResponse {
    private UUID id;
    private String title;
    private TitleType type;
    private String synopsis;
    private Integer releaseYear;
    private String maturityRating;
    private String thumbnailUrl;
    private String posterUrl;
    private String bannerUrl;
    private PublicationState publicationState;
    private boolean mediaReady;
    private Set<GenreResponse> genres;
    private List<SeasonResponse> seasons;
    private Instant createdAt;
    private Instant updatedAt;
}
