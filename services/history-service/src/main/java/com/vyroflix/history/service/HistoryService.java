package com.vyroflix.history.service;

import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.history.dto.*;
import com.vyroflix.history.entity.CompletionRecord;
import com.vyroflix.history.entity.ViewingProgress;
import com.vyroflix.history.entity.WatchlistEntry;
import com.vyroflix.history.event.ContentCompletedPayload;
import com.vyroflix.history.event.WatchlistChangedPayload;
import com.vyroflix.history.repository.CompletionRecordRepository;
import com.vyroflix.history.repository.ViewingProgressRepository;
import com.vyroflix.history.repository.WatchlistEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class HistoryService {

    public static final double COMPLETION_THRESHOLD_PERCENT = 90.0;

    private final ViewingProgressRepository viewingProgressRepository;
    private final WatchlistEntryRepository watchlistEntryRepository;
    private final CompletionRecordRepository completionRecordRepository;
    private final OutboxService outboxService;

    public HistoryService(
            ViewingProgressRepository viewingProgressRepository,
            WatchlistEntryRepository watchlistEntryRepository,
            CompletionRecordRepository completionRecordRepository,
            @Autowired(required = false) OutboxService outboxService) {
        this.viewingProgressRepository = viewingProgressRepository;
        this.watchlistEntryRepository = watchlistEntryRepository;
        this.completionRecordRepository = completionRecordRepository;
        this.outboxService = outboxService;
    }

    /**
     * Idempotently records or updates playback progress for a user.
     * If progress reaches or exceeds 90%, marks as completed and emits a completion event.
     */
    @Transactional
    public ViewingProgressResponse recordProgress(UUID userId, RecordProgressRequest request) {
        log.debug("Recording progress for user [{}] on content [{}] ({}/{}s)",
                userId, request.getContentId(), request.getPositionSeconds(), request.getDurationSeconds());

        double progressPercent = ((double) request.getPositionSeconds() / request.getDurationSeconds()) * 100.0;
        progressPercent = Math.min(100.0, Math.max(0.0, progressPercent));
        boolean isCompleted = progressPercent >= COMPLETION_THRESHOLD_PERCENT;

        Instant now = Instant.now();

        Optional<ViewingProgress> existingOpt = viewingProgressRepository.findByUserIdAndContentId(userId, request.getContentId());
        ViewingProgress progress;

        if (existingOpt.isPresent()) {
            progress = existingOpt.get();
            progress.setPositionSeconds(request.getPositionSeconds());
            progress.setDurationSeconds(request.getDurationSeconds());
            progress.setProgressPercent(progressPercent);
            progress.setTargetType(request.getTargetType());
            progress.setLastWatchedAt(now);
            if (isCompleted || progress.isCompleted()) {
                progress.setCompleted(true);
            }
        } else {
            progress = ViewingProgress.builder()
                    .userId(userId)
                    .contentId(request.getContentId())
                    .targetType(request.getTargetType())
                    .positionSeconds(request.getPositionSeconds())
                    .durationSeconds(request.getDurationSeconds())
                    .progressPercent(progressPercent)
                    .isCompleted(isCompleted)
                    .lastWatchedAt(now)
                    .build();
        }

        ViewingProgress saved = viewingProgressRepository.save(progress);

        // Check if completion record should be created and event emitted
        if (saved.isCompleted() && !completionRecordRepository.existsByUserIdAndContentId(userId, request.getContentId())) {
            CompletionRecord completionRecord = CompletionRecord.builder()
                    .userId(userId)
                    .contentId(request.getContentId())
                    .completedAt(now)
                    .build();
            completionRecordRepository.save(completionRecord);
            publishContentCompletedEvent(userId, request.getContentId(), request.getTargetType(), now);
            log.info("Content [{}] marked completed for user [{}] at {}%", request.getContentId(), userId, progressPercent);
        }

        return ViewingProgressResponse.from(saved);
    }

    /**
     * Retrieves in-progress (uncompleted) content for Continue Watching rail, ordered by most recent.
     */
    @Transactional(readOnly = true)
    public List<ViewingProgressResponse> getContinueWatching(UUID userId, int limit) {
        int boundedLimit = Math.min(Math.max(1, limit), 50);
        return viewingProgressRepository.findByUserIdAndIsCompletedFalseOrderByLastWatchedAtDesc(
                        userId, PageRequest.of(0, boundedLimit))
                .stream()
                .map(ViewingProgressResponse::from)
                .toList();
    }

    /**
     * The viewer's saved progress for one playable id, used by Streaming-Service to resume playback.
     *
     * @throws com.vyroflix.common.error.ApiException NOT_FOUND when the viewer has no progress for it
     */
    @Transactional(readOnly = true)
    public ViewingProgressResponse getProgress(UUID userId, UUID contentId) {
        return viewingProgressRepository.findByUserIdAndContentId(userId, contentId)
                .map(ViewingProgressResponse::from)
                .orElseThrow(() -> new com.vyroflix.common.error.ApiException(
                        com.vyroflix.common.error.ErrorCode.NOT_FOUND, "No viewing progress for this content"));
    }

    /**
     * Adds a title to the user's watchlist idempotently.
     */
    @Transactional
    public WatchlistEntryResponse addToWatchlist(UUID userId, AddToWatchlistRequest request) {
        Optional<WatchlistEntry> existing = watchlistEntryRepository.findByUserIdAndTitleId(userId, request.getTitleId());
        if (existing.isPresent()) {
            return WatchlistEntryResponse.from(existing.get());
        }

        WatchlistEntry entry = WatchlistEntry.builder()
                .userId(userId)
                .titleId(request.getTitleId())
                .createdAt(Instant.now())
                .build();

        WatchlistEntry saved = watchlistEntryRepository.save(entry);
        publishWatchlistChangedEvent(userId, request.getTitleId(), "ADDED", saved.getCreatedAt());

        log.info("Added title [{}] to watchlist for user [{}]", request.getTitleId(), userId);
        return WatchlistEntryResponse.from(saved);
    }

    /**
     * Retrieves the user's watchlist entries ordered by added date descending.
     */
    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> getWatchlist(UUID userId, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 50);

        return watchlistEntryRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(boundedPage, boundedSize))
                .stream()
                .map(WatchlistEntryResponse::from)
                .toList();
    }

    /**
     * Removes a title from the user's watchlist idempotently.
     */
    @Transactional
    public void removeFromWatchlist(UUID userId, UUID titleId) {
        Optional<WatchlistEntry> existing = watchlistEntryRepository.findByUserIdAndTitleId(userId, titleId);
        if (existing.isPresent()) {
            watchlistEntryRepository.delete(existing.get());
            publishWatchlistChangedEvent(userId, titleId, "REMOVED", Instant.now());
            log.info("Removed title [{}] from watchlist for user [{}]", titleId, userId);
        }
    }

    /**
     * Checks if a title is currently in the user's watchlist.
     */
    @Transactional(readOnly = true)
    public WatchlistStatusResponse isInWatchlist(UUID userId, UUID titleId) {
        boolean exists = watchlistEntryRepository.existsByUserIdAndTitleId(userId, titleId);
        return WatchlistStatusResponse.builder()
                .titleId(titleId)
                .inWatchlist(exists)
                .build();
    }

    private void publishContentCompletedEvent(UUID userId, UUID contentId, com.vyroflix.history.entity.TargetType targetType, Instant completedAt) {
        if (outboxService == null) {
            return;
        }

        ContentCompletedPayload payload = ContentCompletedPayload.builder()
                .userId(userId)
                .contentId(contentId)
                .targetType(targetType)
                .completedAt(completedAt)
                .build();

        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_HISTORY_CONTENT_COMPLETED,
                Constants.SERVICE_HISTORY,
                UUID.randomUUID(),
                payload
        ));
    }

    private void publishWatchlistChangedEvent(UUID userId, UUID titleId, String action, Instant occurredAt) {
        if (outboxService == null) {
            return;
        }

        WatchlistChangedPayload payload = WatchlistChangedPayload.builder()
                .userId(userId)
                .titleId(titleId)
                .action(action)
                .occurredAt(occurredAt)
                .build();

        String topic = "ADDED".equalsIgnoreCase(action)
                ? Constants.TOPIC_WATCHLIST_ITEM_ADDED
                : Constants.TOPIC_WATCHLIST_ITEM_REMOVED;

        outboxService.save(EventEnvelope.of(
                topic,
                Constants.SERVICE_HISTORY,
                UUID.randomUUID(),
                payload
        ));
    }
}
