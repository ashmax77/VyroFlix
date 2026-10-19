package com.vyroflix.content.event;

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
public class TitleUnpublishedPayload {
    private UUID titleId;
    private Instant unpublishedAt;
}
