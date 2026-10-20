package com.vyroflix.video.dto;

import com.vyroflix.video.entity.TargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUploadIntentRequest {

    @NotNull(message = "contentId is required")
    private UUID contentId;

    @NotNull(message = "targetType is required")
    private TargetType targetType;

    @NotBlank(message = "filename is required")
    private String filename;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @Min(value = 1, message = "fileSizeBytes must be greater than zero")
    private Long fileSizeBytes;
}
