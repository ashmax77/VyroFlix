package com.vyroflix.content.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTitleRequest {

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String synopsis;

    private Integer releaseYear;

    @Size(max = 10, message = "Maturity rating cannot exceed 10 characters")
    private String maturityRating;

    private String thumbnailUrl;

    private String posterUrl;

    private String bannerUrl;

    private Set<UUID> genreIds;
}
