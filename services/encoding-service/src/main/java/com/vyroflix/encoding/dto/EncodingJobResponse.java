package com.vyroflix.encoding.dto;

import com.vyroflix.encoding.entity.EncodingJob;
import com.vyroflix.encoding.entity.EncodingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EncodingJobResponse {
    private final UUID id;
    private final UUID uploadIntentId;
    private final UUID contentId;
    private final String rawS3Bucket;
    private final String rawS3Key;
    private final String outputS3Bucket;
    private final String outputS3Prefix;
    private final String masterPlaylistKey;
    private final EncodingStatus status;
    private final Integer durationSeconds;
    private final String errorMessage;
    private final int retryCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<HlsRenditionResponse> renditions;

    public static EncodingJobResponse from(EncodingJob job) {
        if (job == null) {
            return null;
        }

        List<HlsRenditionResponse> renditionResponses = job.getRenditions() != null
                ? job.getRenditions().stream().map(HlsRenditionResponse::from).toList()
                : Collections.emptyList();

        return EncodingJobResponse.builder()
                .id(job.getId())
                .uploadIntentId(job.getUploadIntentId())
                .contentId(job.getContentId())
                .rawS3Bucket(job.getRawS3Bucket())
                .rawS3Key(job.getRawS3Key())
                .outputS3Bucket(job.getOutputS3Bucket())
                .outputS3Prefix(job.getOutputS3Prefix())
                .masterPlaylistKey(job.getMasterPlaylistKey())
                .status(job.getStatus())
                .durationSeconds(job.getDurationSeconds())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .renditions(renditionResponses)
                .build();
    }
}
