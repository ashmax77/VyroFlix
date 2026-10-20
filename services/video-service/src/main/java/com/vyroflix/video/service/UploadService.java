package com.vyroflix.video.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoUploadedEventPayload;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import com.vyroflix.video.dto.CreateUploadIntentRequest;
import com.vyroflix.video.dto.UploadIntentResponse;
import com.vyroflix.video.entity.UploadIntent;
import com.vyroflix.video.entity.UploadStatus;
import com.vyroflix.video.repository.UploadIntentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class UploadService {

    public static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(30);

    private final UploadIntentRepository uploadIntentRepository;
    private final ObjectStorageClient storageClient;
    private final OutboxService outboxService;

    public UploadService(
            UploadIntentRepository uploadIntentRepository,
            @Autowired(required = false) ObjectStorageClient storageClient,
            @Autowired(required = false) OutboxService outboxService) {
        this.uploadIntentRepository = uploadIntentRepository;
        this.storageClient = storageClient;
        this.outboxService = outboxService;
    }

    /**
     * Creates an upload intent and generates a pre-signed PUT URL for direct client upload to S3/MinIO.
     */
    @Transactional
    public UploadIntentResponse createUploadIntent(CreateUploadIntentRequest request) {
        log.info("Creating upload intent for content [{}] targetType [{}] filename [{}]",
                request.getContentId(), request.getTargetType(), request.getFilename());

        String sanitizedFilename = sanitizeFilename(request.getFilename());
        String s3Key = String.format("raw/%s/%s/%s/%s",
                request.getTargetType().name().toLowerCase(),
                request.getContentId(),
                UUID.randomUUID(),
                sanitizedFilename);

        Instant expiresAt = Instant.now().plus(UPLOAD_URL_TTL);
        String presignedUrl = generatePresignedUploadUrl(s3Key, request.getContentType(), UPLOAD_URL_TTL);

        UploadIntent intent = UploadIntent.builder()
                .contentId(request.getContentId())
                .targetType(request.getTargetType())
                .filename(sanitizedFilename)
                .contentType(request.getContentType())
                .fileSizeBytes(request.getFileSizeBytes())
                .s3Bucket(Constants.BUCKET_RAW)
                .s3Key(s3Key)
                .status(UploadStatus.PENDING)
                .uploadUrl(presignedUrl)
                .expiresAt(expiresAt)
                .build();

        UploadIntent saved = uploadIntentRepository.save(intent);
        log.info("Upload intent [{}] created with key [{}]", saved.getId(), s3Key);

        return UploadIntentResponse.from(saved);
    }

    /**
     * Completes an upload intent, verifies the object exists in storage, and emits video.uploaded.v1.
     */
    @Transactional
    public UploadIntentResponse completeUploadIntent(UUID intentId) {
        log.info("Verifying and completing upload intent [{}]", intentId);

        UploadIntent intent = uploadIntentRepository.findById(intentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Upload intent not found: " + intentId));

        if (intent.getStatus() == UploadStatus.UPLOADED) {
            log.info("Upload intent [{}] is already marked as UPLOADED", intentId);
            return UploadIntentResponse.from(intent);
        }

        // Verify that the file was actually uploaded to storage
        if (storageClient != null) {
            boolean exists = false;
            try {
                exists = storageClient.doesObjectExist(intent.getS3Bucket(), intent.getS3Key());
            } catch (Exception e) {
                log.warn("Storage existence check encountered an error for key [{}]: {}", intent.getS3Key(), e.getMessage());
            }

            if (!exists) {
                intent.setStatus(UploadStatus.FAILED);
                uploadIntentRepository.save(intent);
                throw new ApiException(ErrorCode.BAD_REQUEST, "Uploaded object does not exist in storage bucket: " + intent.getS3Key());
            }
        }

        Instant uploadedAt = Instant.now();
        intent.setStatus(UploadStatus.UPLOADED);
        UploadIntent saved = uploadIntentRepository.save(intent);

        // Publish video.uploaded.v1 event via transactional outbox
        publishVideoUploadedEvent(saved, uploadedAt);

        log.info("Upload intent [{}] completed successfully and published to outbox", intentId);
        return UploadIntentResponse.from(saved);
    }

    /**
     * Retrieves upload intent status by ID.
     */
    @Transactional(readOnly = true)
    public UploadIntentResponse getUploadIntent(UUID intentId) {
        UploadIntent intent = uploadIntentRepository.findById(intentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Upload intent not found: " + intentId));
        return UploadIntentResponse.from(intent);
    }

    @Transactional(readOnly = true)
    public java.util.List<UploadIntentResponse> getIntentsByContentId(UUID contentId) {
        return uploadIntentRepository.findByContentId(contentId).stream()
                .map(UploadIntentResponse::from)
                .toList();
    }

    private String generatePresignedUploadUrl(String s3Key, String contentType, Duration ttl) {
        if (storageClient != null) {
            try {
                return storageClient.generatePresignedUploadUrl(Constants.BUCKET_RAW, s3Key, ttl, contentType);
            } catch (Exception e) {
                log.error("Failed to generate presigned upload URL for key [{}]: {}", s3Key, e.getMessage(), e);
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to generate signed upload URL", e);
            }
        }

        return "http://localhost:9000/" + Constants.BUCKET_RAW + "/" + s3Key;
    }

    private void publishVideoUploadedEvent(UploadIntent intent, Instant uploadedAt) {
        if (outboxService == null) {
            return;
        }

        VideoUploadedEventPayload payload = VideoUploadedEventPayload.builder()
                .uploadIntentId(intent.getId())
                .contentId(intent.getContentId())
                .targetType(intent.getTargetType().name())
                .s3Bucket(intent.getS3Bucket())
                .s3Key(intent.getS3Key())
                .filename(intent.getFilename())
                .fileSizeBytes(intent.getFileSizeBytes())
                .uploadedAt(uploadedAt)
                .build();

        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_VIDEO_UPLOADED,
                Constants.SERVICE_VIDEO,
                UUID.randomUUID(),
                payload
        ));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "video.mp4";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
