package com.vyroflix.streaming.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.client.HistoryProgressClient;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.entity.MasterPlaylistKey;
import com.vyroflix.streaming.entity.PlaybackSession;
import com.vyroflix.streaming.entity.TitleAvailability;
import com.vyroflix.streaming.event.PlaybackEndedPayload;
import com.vyroflix.streaming.event.PlaybackStartedPayload;
import com.vyroflix.streaming.repository.MasterPlaylistKeyRepository;
import com.vyroflix.streaming.repository.PlaybackSessionRepository;
import com.vyroflix.streaming.repository.TitleAvailabilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Authorises playback: published availability, encoded media, plan-based concurrency, then a short-lived
 * manifest URL. Never serves or proxies media.
 *
 * <p>Authorisation data comes from this service's own read models (fed by catalog and encoding events), so
 * playback does not depend on Content-Service being reachable.</p>
 */
@Slf4j
@Service
public class StreamingService {

    public static final Duration PLAYBACK_SESSION_TTL = Duration.ofMinutes(15);
    public static final String REDIS_PLAYLIST_KEY_PREFIX = "content:master-key:";

    static final String TARGET_MOVIE = "MOVIE";
    static final String TARGET_EPISODE = "EPISODE";
    static final Map<String, Integer> CONCURRENT_STREAM_LIMITS = Map.of(
            Constants.PLAN_FREE, 1,
            Constants.PLAN_BASIC, 2,
            Constants.PLAN_PREMIUM, 4);

    private static final Duration REDIS_KEY_TTL = Duration.ofHours(24);
    private static final Duration REDIS_LOOKUP_CACHE_TTL = Duration.ofHours(1);

    private final PlaybackSessionRepository playbackSessionRepository;
    private final TitleAvailabilityRepository availabilityRepository;
    private final MasterPlaylistKeyRepository masterPlaylistKeyRepository;
    private final PlaybackUrlSigner urlSigner;
    private final HistoryProgressClient historyProgressClient;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    private final OutboxService outboxService;

    public StreamingService(
            PlaybackSessionRepository playbackSessionRepository,
            TitleAvailabilityRepository availabilityRepository,
            MasterPlaylistKeyRepository masterPlaylistKeyRepository,
            PlaybackUrlSigner urlSigner,
            HistoryProgressClient historyProgressClient,
            TransactionTemplate transactionTemplate,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) OutboxService outboxService) {
        this.playbackSessionRepository = playbackSessionRepository;
        this.availabilityRepository = availabilityRepository;
        this.masterPlaylistKeyRepository = masterPlaylistKeyRepository;
        this.urlSigner = urlSigner;
        this.historyProgressClient = historyProgressClient;
        this.transactionTemplate = transactionTemplate;
        this.redisTemplate = redisTemplate;
        this.outboxService = outboxService;
    }

    /**
     * Creates an auditable playback session. Not annotated {@code @Transactional}: the resume lookup is a
     * remote call and must not hold a database connection; the write runs in a short transaction instead.
     *
     * @param authorizationHeader the caller's bearer token, forwarded to History for the resume lookup
     */
    public PlaybackSessionResponse createSession(
            UUID userId,
            String plan,
            String authorizationHeader,
            CreateSessionRequest request,
            String ipAddress) {

        UUID contentId = request.getContentId();
        String targetType = normalizeTargetType(request.getTargetType());
        Instant now = Instant.now();

        requireAvailable(contentId, targetType);

        String playlistKey = resolveMasterPlaylistKey(contentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ASSET_NOT_READY, "Media is not ready for playback"));

        int resumeAt = historyProgressClient.resumePositionSeconds(contentId, authorizationHeader);
        String manifestUrl = urlSigner.sign(playlistKey, PLAYBACK_SESSION_TTL);
        Instant expiresAt = now.plus(PLAYBACK_SESSION_TTL);

        PlaybackSession saved = transactionTemplate.execute(status -> {
            enforceConcurrencyLimit(userId, plan, now);
            PlaybackSession session = playbackSessionRepository.save(PlaybackSession.builder()
                    .userId(userId)
                    .contentId(contentId)
                    .targetType(targetType)
                    .deviceId(request.getDeviceId())
                    .ipAddress(ipAddress)
                    .masterPlaylistUrl(playlistKey)
                    .resumePositionSeconds(resumeAt)
                    .expiresAt(expiresAt)
                    .build());
            publishPlaybackStartedEvent(session);
            return session;
        });

        log.info("Playback session [{}] created for content [{}] (plan={}, resume={}s)",
                saved.getId(), contentId, plan, resumeAt);

        return PlaybackSessionResponse.builder()
                .sessionId(saved.getId())
                .manifestUrl(manifestUrl)
                .expiresAt(expiresAt)
                .resumePositionSeconds(resumeAt)
                .build();
    }

    /**
     * Ends an open session owned by the caller. Idempotent: ending an ended session is a no-op.
     */
    public void endSession(UUID userId, UUID sessionId) {
        transactionTemplate.executeWithoutResult(status -> {
            PlaybackSession session = playbackSessionRepository.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND, "Playback session not found"));
            if (session.getEndedAt() != null) {
                return;
            }
            session.setEndedAt(Instant.now());
            playbackSessionRepository.save(session);
            publishPlaybackEndedEvent(session);
        });
    }

    /** Records a title or episode's publication state from catalog events. */
    public void recordTitlePublication(UUID titleId, boolean published) {
        transactionTemplate.executeWithoutResult(status -> upsertAvailability(titleId, null, published));
    }

    /** Records an episode as published under its series (catalog.episode.published.v1). */
    public void recordEpisodePublication(UUID episodeId, UUID titleId) {
        transactionTemplate.executeWithoutResult(status -> upsertAvailability(episodeId, titleId, true));
    }

    /** Stores the master playlist key from video.encoded.v1 (DB as source of truth, Redis as cache). */
    public void registerMasterPlaylistKey(UUID contentId, String masterPlaylistKey) {
        if (contentId == null || masterPlaylistKey == null || masterPlaylistKey.isBlank()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            MasterPlaylistKey row = masterPlaylistKeyRepository.findById(contentId)
                    .orElseGet(() -> MasterPlaylistKey.builder().contentId(contentId).build());
            row.setPlaylistKey(masterPlaylistKey);
            row.setUpdatedAt(Instant.now());
            masterPlaylistKeyRepository.save(row);
        });
        cacheInRedis(contentId, masterPlaylistKey, REDIS_KEY_TTL);
        log.info("Registered master playlist key for content [{}]", contentId);
    }

    private void requireAvailable(UUID contentId, String targetType) {
        TitleAvailability row = availabilityRepository.findById(contentId).orElse(null);
        boolean available;
        if (row == null || !row.isPublished()) {
            available = false;
        } else if (TARGET_EPISODE.equals(targetType)) {
            available = row.getParentId() != null
                    && availabilityRepository.findById(row.getParentId())
                            .map(TitleAvailability::isPublished)
                            .orElse(false);
        } else {
            available = row.getParentId() == null;
        }
        if (!available) {
            // Same response for "unknown" and "unpublished" so drafts are not enumerable.
            throw new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title is not available for playback");
        }
    }

    private void enforceConcurrencyLimit(UUID userId, String plan, Instant now) {
        int limit = CONCURRENT_STREAM_LIMITS.getOrDefault(normalizePlan(plan), 1);
        long active = playbackSessionRepository.countByUserIdAndEndedAtIsNullAndExpiresAtAfter(userId, now);
        if (active >= limit) {
            throw new ApiException(ErrorCode.CONCURRENT_STREAM_LIMIT,
                    "Your plan allows " + limit + " concurrent stream(s). End another stream and try again.");
        }
    }

    private void upsertAvailability(UUID id, UUID parentId, boolean published) {
        TitleAvailability row = availabilityRepository.findById(id)
                .orElseGet(() -> TitleAvailability.builder().contentId(id).build());
        row.setParentId(parentId);
        row.setPublished(published);
        row.setUpdatedAt(Instant.now());
        availabilityRepository.save(row);
    }

    private Optional<String> resolveMasterPlaylistKey(UUID contentId) {
        String cacheKey = REDIS_PLAYLIST_KEY_PREFIX + contentId;
        String cached = readRedis(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> stored = masterPlaylistKeyRepository.findById(contentId)
                .map(MasterPlaylistKey::getPlaylistKey);
        stored.ifPresent(key -> cacheInRedis(contentId, key, REDIS_LOOKUP_CACHE_TTL));
        return stored;
    }

    private String readRedis(String cacheKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            return value == null || value.isBlank() ? null : value;
        } catch (Exception e) {
            log.warn("Redis lookup failed; falling back to database: {}", e.getMessage());
            return null;
        }
    }

    private void cacheInRedis(UUID contentId, String key, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(REDIS_PLAYLIST_KEY_PREFIX + contentId, key, ttl.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache master playlist key in Redis: {}", e.getMessage());
        }
    }

    private static String normalizeTargetType(String requested) {
        if (requested == null || requested.isBlank()) {
            return TARGET_MOVIE;
        }
        String upper = requested.trim().toUpperCase();
        if (!TARGET_MOVIE.equals(upper) && !TARGET_EPISODE.equals(upper)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "targetType must be MOVIE or EPISODE");
        }
        return upper;
    }

    static String normalizePlan(String plan) {
        if (plan == null) {
            return Constants.PLAN_FREE;
        }
        String upper = plan.trim().toUpperCase();
        return CONCURRENT_STREAM_LIMITS.containsKey(upper) ? upper : Constants.PLAN_FREE;
    }

    private void publishPlaybackStartedEvent(PlaybackSession session) {
        if (outboxService == null) {
            return;
        }
        PlaybackStartedPayload payload = PlaybackStartedPayload.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .contentId(session.getContentId())
                .targetType(session.getTargetType())
                .startedAt(session.getCreatedAt() != null ? session.getCreatedAt() : Instant.now())
                .build();
        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_PLAYBACK_STARTED,
                Constants.SERVICE_STREAMING,
                UUID.randomUUID(),
                payload));
    }

    private void publishPlaybackEndedEvent(PlaybackSession session) {
        if (outboxService == null) {
            return;
        }
        PlaybackEndedPayload payload = PlaybackEndedPayload.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .contentId(session.getContentId())
                .endedAt(session.getEndedAt())
                .build();
        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_PLAYBACK_ENDED,
                Constants.SERVICE_STREAMING,
                UUID.randomUUID(),
                payload));
    }
}
