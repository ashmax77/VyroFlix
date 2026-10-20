package com.vyroflix.encoding.controller;

import com.vyroflix.encoding.dto.EncodingJobResponse;
import com.vyroflix.encoding.entity.EncodingStatus;
import com.vyroflix.encoding.service.EncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Administrative and monitoring REST controller for encoding jobs.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/encoding/jobs")
@RequiredArgsConstructor
public class EncodingJobController {

    private final EncodingService encodingService;

    /**
     * Lists encoding jobs with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<EncodingJobResponse>> getJobs(
            @RequestParam(value = "status", required = false) EncodingStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<EncodingJobResponse> jobs = encodingService.getJobs(status, pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Retrieves details and renditions for a specific encoding job.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EncodingJobResponse> getJob(
            @PathVariable("id") UUID id) {
        EncodingJobResponse response = encodingService.getJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves encoding jobs associated with a content ID (movie or episode).
     */
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<EncodingJobResponse>> getJobsByContent(
            @PathVariable("contentId") UUID contentId) {
        List<EncodingJobResponse> responses = encodingService.getJobsByContentId(contentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retries a failed or stuck encoding job.
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<EncodingJobResponse> retryJob(
            @PathVariable("id") UUID id) {
        log.info("Received request to retry encoding job [{}]", id);
        EncodingJobResponse response = encodingService.retryJob(id);
        return ResponseEntity.ok(response);
    }
}
