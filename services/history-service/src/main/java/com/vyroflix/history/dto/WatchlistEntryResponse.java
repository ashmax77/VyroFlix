package com.vyroflix.history.dto;

import com.vyroflix.history.entity.WatchlistEntry;
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
public class WatchlistEntryResponse {

    private UUID id;
    private UUID userId;
    private UUID titleId;
    private Instant createdAt;

    public static WatchlistEntryResponse from(WatchlistEntry entity) {
        return WatchlistEntryResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .titleId(entity.getTitleId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
