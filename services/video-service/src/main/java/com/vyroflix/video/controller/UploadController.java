package com.vyroflix.video.controller;

import com.vyroflix.video.dto.CreateUploadIntentRequest;
import com.vyroflix.video.dto.UploadIntentResponse;
import com.vyroflix.video.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for video upload operations.
 *
 * <p>Handles direct client upload coordination to Cloudflare R2 / MinIO via short-lived
 * presigned PUT URLs, per AGENTS.md. Video bytes NEVER pass through Spring Boot or API Gateway.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos/upload-intents")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * Initializes a new direct upload intent and issues a presigned S3/R2 upload URL.
     */
    @PostMapping
    public ResponseEntity<UploadIntentResponse> createUploadIntent(
            @Valid @RequestBody CreateUploadIntentRequest request) {
        log.info("Received request to create upload intent for content [{}] filename [{}]",
                request.getContentId(), request.getFilename());
        UploadIntentResponse response = uploadService.createUploadIntent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirms that the client finished uploading to S3/R2.
     * Verifies object presence in the bucket and queues background transcoding via transactional outbox.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<UploadIntentResponse> completeUploadIntent(
            @PathVariable("id") UUID id) {
        log.info("Received complete notification for upload intent [{}]", id);
        UploadIntentResponse response = uploadService.completeUploadIntent(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Queries status and details of an upload intent.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UploadIntentResponse> getUploadIntent(
            @PathVariable("id") UUID id) {
        UploadIntentResponse response = uploadService.getUploadIntent(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all upload intents associated with a specific movie or episode.
     */
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<UploadIntentResponse>> getIntentsByContent(
            @PathVariable("contentId") UUID contentId) {
        List<UploadIntentResponse> responses = uploadService.getIntentsByContentId(contentId);
        return ResponseEntity.ok(responses);
    }
}
