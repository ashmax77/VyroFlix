package com.vyroflix.history.dto;

import com.vyroflix.history.entity.TargetType;
import com.vyroflix.history.entity.ViewingProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewingProgressResponse {

    private UUID id;
    private UUID userId;
    private UUID contentId;
    private TargetType targetType;
    private int positionSeconds;
    private int durationSeconds;
    private double progressPercent;
    private boolean completed;
    private Instant lastWatchedAt;

    public static ViewingProgressResponse from(ViewingProgress entity) {
        return ViewingProgressResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .contentId(entity.getContentId())
                .targetType(entity.getTargetType())
                .positionSeconds(entity.getPositionSeconds())
                .durationSeconds(entity.getDurationSeconds())
                .progressPercent(entity.getProgressPercent())
                .completed(entity.isCompleted())
                .lastWatchedAt(entity.getLastWatchedAt())
                .build();
    }
}
