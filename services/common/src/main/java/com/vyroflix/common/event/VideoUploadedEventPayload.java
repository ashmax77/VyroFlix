package com.vyroflix.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for {@code video.uploaded.v1} event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEventPayload {

    private UUID uploadIntentId;
    private UUID contentId;
    private String targetType;
    private String s3Bucket;
    private String s3Key;
    private String filename;
    private Long fileSizeBytes;
    private Instant uploadedAt;
}
