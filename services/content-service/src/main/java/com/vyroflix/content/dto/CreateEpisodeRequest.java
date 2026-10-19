package com.vyroflix.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEpisodeRequest {

    @NotNull(message = "Episode number is required")
    @Min(value = 1, message = "Episode number must be at least 1")
    private Integer episodeNumber;

    @NotBlank(message = "Episode name is required")
    private String name;

    private String overview;

    private Integer durationSeconds;

    private String thumbnailUrl;
}
