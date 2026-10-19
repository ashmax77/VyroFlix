package com.vyroflix.streaming.dto;

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
public class PlaybackSessionResponse {

    private UUID sessionId;
    private String manifestUrl;
    private Instant expiresAt;
    private int resumePositionSeconds;
}
