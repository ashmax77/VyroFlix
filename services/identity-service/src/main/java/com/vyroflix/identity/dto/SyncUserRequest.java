package com.vyroflix.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100, message = "Display name cannot exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    private String avatarUrl;
}
