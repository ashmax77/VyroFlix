package com.vyroflix.video.event;

import com.vyroflix.video.entity.TargetType;
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
public class VideoUploadedPayload {

    private UUID uploadIntentId;
    private UUID contentId;
    private TargetType targetType;
    private String s3Bucket;
    private String s3Key;
    private String filename;
    private Long fileSizeBytes;
    private Instant uploadedAt;
}
