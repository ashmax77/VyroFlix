package com.vyroflix.search.dto;

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
public class SearchCardResponse {
    private UUID id;
    private String title;
    private String type;
    private String synopsis;
    private Integer releaseYear;
    private String maturityRating;
    private List<String> genres;
    private List<String> castMembers;
    private List<String> directors;
    private String posterUrl;
}
