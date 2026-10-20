package com.vyroflix.history.event;

import com.vyroflix.history.entity.TargetType;
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
public class ContentCompletedPayload {

    private UUID userId;
    private UUID contentId;
    private TargetType targetType;
    private Instant completedAt;
}
