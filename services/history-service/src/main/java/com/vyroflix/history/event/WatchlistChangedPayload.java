package com.vyroflix.history.event;

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
public class WatchlistChangedPayload {

    private UUID userId;
    private UUID titleId;
    private String action; // ADDED or REMOVED
    private Instant occurredAt;
}
