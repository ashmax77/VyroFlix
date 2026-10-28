package com.vyroflix.content.dto;

import jakarta.validation.Valid;
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

/** Request and response shapes for people and title credits. */
public final class PersonDtos {

    private PersonDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonResponse {
        private UUID id;
        private String name;
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePersonRequest {
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name cannot exceed 255 characters")
        private String name;

        @Size(max = 500, message = "avatarUrl cannot exceed 500 characters")
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCreditsRequest {
        /** Replaces the title's full credit list. */
        @NotNull
        @Valid
        @Size(max = 100, message = "A title can have at most 100 credits")
        private List<CreditEntry> credits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditEntry {
        @NotNull(message = "personId is required")
        private UUID personId;

        @NotBlank(message = "role is required")
        @Pattern(regexp = "ACTOR|DIRECTOR|WRITER|PRODUCER", message = "role must be ACTOR, DIRECTOR, WRITER or PRODUCER")
        private String role;

        @Size(max = 255, message = "characterName cannot exceed 255 characters")
        private String characterName;
    }
}
