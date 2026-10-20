package com.vyroflix.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemResponse {
    private UUID titleId;
    private String title;
    private String type;
    private List<String> genres;
    private String posterUrl;
    private Integer releaseYear;
    private String maturityRating;
    private double score;
    private String reason;
}
