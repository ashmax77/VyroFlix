package com.vyroflix.recommendation.service;

import com.vyroflix.recommendation.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Applies counter-style events (view/completion counts) exactly once per {@code eventId}.
 *
 * <p>The dedupe claim and the counter update share one transaction: if the update fails the claim rolls
 * back, so the redelivered event is processed again instead of being silently lost.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEventProcessor {

    static final String CONSUMER_PLAYBACK_STARTED = "recommendation.playback-started";
    static final String CONSUMER_CONTENT_COMPLETED = "recommendation.content-completed";

    private final RecommendationService recommendationService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void onPlaybackStarted(String eventId, UUID titleId) {
        if (alreadyProcessed(eventId, CONSUMER_PLAYBACK_STARTED)) {
            return;
        }
        recommendationService.recordPlaybackStarted(titleId);
    }

    @Transactional
    public void onContentCompleted(String eventId, UUID titleId) {
        if (alreadyProcessed(eventId, CONSUMER_CONTENT_COMPLETED)) {
            return;
        }
        recommendationService.recordViewingProgress(titleId, true);
    }

    /** Events without an id (legacy raw payloads) cannot be deduplicated and are applied as-is. */
    private boolean alreadyProcessed(String eventId, String consumer) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        boolean duplicate = processedEventRepository.claim(eventId, consumer) == 0;
        if (duplicate) {
            log.info("Skipping duplicate event eventId=[{}] consumer=[{}]", eventId, consumer);
        }
        return duplicate;
    }
}
