package com.vyroflix.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeasonRequest {

    @NotNull(message = "Season number is required")
    @Min(value = 1, message = "Season number must be at least 1")
    private Integer seasonNumber;

    private String name;

    private String overview;
}
