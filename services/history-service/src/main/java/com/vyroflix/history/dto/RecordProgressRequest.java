package com.vyroflix.history.dto;

import com.vyroflix.history.entity.TargetType;
import jakarta.validation.constraints.Min;
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
public class RecordProgressRequest {

    @NotNull(message = "contentId is required")
    private UUID contentId;

    @NotNull(message = "targetType is required")
    private TargetType targetType;

    @Min(value = 0, message = "positionSeconds must be non-negative")
    private int positionSeconds;

    @Min(value = 1, message = "durationSeconds must be greater than zero")
    private int durationSeconds;
}
