package com.vyroflix.streaming.event;

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
public class PlaybackStartedPayload {

    private UUID sessionId;
    private UUID userId;
    private UUID contentId;
    private String targetType;
    private Instant startedAt;
}
