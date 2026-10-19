package com.vyroflix.streaming.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.entity.PlaybackSession;
import com.vyroflix.streaming.event.PlaybackStartedPayload;
import com.vyroflix.streaming.repository.PlaybackSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StreamingService {

    public static final Duration PLAYBACK_SESSION_TTL = Duration.ofMinutes(15);
    public static final String REDIS_PLAYLIST_KEY_PREFIX = "content:master-key:";

    private final PlaybackSessionRepository playbackSessionRepository;
    private final ObjectStorageClient storageClient;
    private final StringRedisTemplate redisTemplate;
    private final OutboxService outboxService;

    public StreamingService(
            PlaybackSessionRepository playbackSessionRepository,
            @Autowired(required = false) ObjectStorageClient storageClient,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) OutboxService outboxService) {
        this.playbackSessionRepository = playbackSessionRepository;
        this.storageClient = storageClient;
        this.redisTemplate = redisTemplate;
        this.outboxService = outboxService;
    }

    @Transactional
    public PlaybackSessionResponse createSession(UUID userId, CreateSessionRequest request, String ipAddress) {
        log.info("Creating playback session for userId={} contentId={} targetType={}",
                userId, request.getContentId(), request.getTargetType());

        String targetType = request.getTargetType() != null && !request.getTargetType().isBlank()
                ? request.getTargetType().toUpperCase()
                : "MOVIE";

        // 1. Resolve master playlist object key (using Redis cache if present)
        String playlistKey = resolveMasterPlaylistKey(request.getContentId(), targetType);

        // 2. Generate short-lived signed manifest URL
        Instant expiresAt = Instant.now().plus(PLAYBACK_SESSION_TTL);
        String manifestUrl = generateManifestUrl(playlistKey, PLAYBACK_SESSION_TTL);

        // 3. Persist PlaybackSession record
        PlaybackSession session = PlaybackSession.builder()
                .userId(userId)
                .contentId(request.getContentId())
                .targetType(targetType)
                .deviceId(request.getDeviceId())
                .ipAddress(ipAddress)
                .masterPlaylistUrl(manifestUrl)
                .resumePositionSeconds(0)
                .expiresAt(expiresAt)
                .build();

        PlaybackSession saved = playbackSessionRepository.save(session);

        // 4. Publish playback.started.v1 event via transactional outbox
        publishPlaybackStartedEvent(saved);

        log.info("Playback session [{}] created successfully for content [{}], expires at [{}]",
                saved.getId(), request.getContentId(), expiresAt);

        return PlaybackSessionResponse.builder()
                .sessionId(saved.getId())
                .manifestUrl(manifestUrl)
                .expiresAt(expiresAt)
                .resumePositionSeconds(saved.getResumePositionSeconds())
                .build();
    }

    private String resolveMasterPlaylistKey(UUID contentId, String targetType) {
        String cacheKey = REDIS_PLAYLIST_KEY_PREFIX + contentId;

        if (redisTemplate != null) {
            try {
                String cachedKey = redisTemplate.opsForValue().get(cacheKey);
                if (cachedKey != null && !cachedKey.isBlank()) {
                    log.debug("Found cached master playlist key in Redis: {}", cachedKey);
                    return cachedKey;
                }
            } catch (Exception e) {
                log.warn("Redis lookup failed for key [{}]: {}", cacheKey, e.getMessage());
            }
        }

        // Formulate standard HLS playlist key path
        String playlistKey;
        if ("EPISODE".equalsIgnoreCase(targetType)) {
            playlistKey = "hls/episodes/" + contentId + "/master.m3u8";
        } else {
            playlistKey = "hls/titles/" + contentId + "/master.m3u8";
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, playlistKey, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Failed to cache master playlist key in Redis: {}", e.getMessage());
            }
        }

        return playlistKey;
    }

    private String generateManifestUrl(String playlistKey, Duration expiry) {
        if (storageClient != null) {
            try {
                return storageClient.generatePresignedDownloadUrl(Constants.BUCKET_CONTENT, playlistKey, expiry);
            } catch (Exception e) {
                log.error("Failed to generate presigned download URL for key [{}]: {}", playlistKey, e.getMessage(), e);
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to generate signed playback URL", e);
            }
        }

        // Fallback for local mock/dev if ObjectStorageClient bean not active
        return "http://localhost:9000/" + Constants.BUCKET_CONTENT + "/" + playlistKey;
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
                payload
        ));
    }
}
