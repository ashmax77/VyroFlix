package com.vyroflix.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Request and response shapes for curated collections. */
public final class CollectionDtos {

    private CollectionDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TitleSummary {
        private UUID id;
        private String title;
        private String type;
        private String posterUrl;
        private String publicationState;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionResponse {
        private UUID id;
        private String name;
        private String slug;
        private int displayOrder;
        private List<TitleSummary> titles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCollectionRequest {
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name cannot exceed 255 characters")
        private String name;

        @NotBlank(message = "slug is required")
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "slug must be lowercase letters, digits and hyphens")
        @Size(max = 255, message = "slug cannot exceed 255 characters")
        private String slug;

        @Min(value = 0, message = "displayOrder must be >= 0")
        @Max(value = 10000, message = "displayOrder must be <= 10000")
        private int displayOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCollectionTitlesRequest {
        /** Replaces the collection's titles. Every id must exist. */
        @NotNull
        @Size(max = 200, message = "A collection can hold at most 200 titles")
        private List<UUID> titleIds;
    }
}
