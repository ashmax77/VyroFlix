package com.vyroflix.video.dto;

import com.vyroflix.video.entity.TargetType;
import com.vyroflix.video.entity.UploadIntent;
import com.vyroflix.video.entity.UploadStatus;
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
public class UploadIntentResponse {

    private UUID id;
    private UUID contentId;
    private TargetType targetType;
    private String filename;
    private String contentType;
    private Long fileSizeBytes;
    private String s3Bucket;
    private String s3Key;
    private UploadStatus status;
    private String uploadUrl;
    private Instant expiresAt;
    private Instant createdAt;

    public static UploadIntentResponse from(UploadIntent entity) {
        return UploadIntentResponse.builder()
                .id(entity.getId())
                .contentId(entity.getContentId())
                .targetType(entity.getTargetType())
                .filename(entity.getFilename())
                .contentType(entity.getContentType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .s3Bucket(entity.getS3Bucket())
                .s3Key(entity.getS3Key())
                .status(entity.getStatus())
                .uploadUrl(entity.getUploadUrl())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
