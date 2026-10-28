package com.vyroflix.streaming.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.client.HistoryProgressClient;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.entity.MasterPlaylistKey;
import com.vyroflix.streaming.entity.PlaybackSession;
import com.vyroflix.streaming.entity.TitleAvailability;
import com.vyroflix.streaming.repository.MasterPlaylistKeyRepository;
import com.vyroflix.streaming.repository.PlaybackSessionRepository;
import com.vyroflix.streaming.repository.TitleAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";
    private static final String CDN = "https://media.vyroflix.test";

    @Mock private PlaybackSessionRepository playbackSessionRepository;
    @Mock private TitleAvailabilityRepository availabilityRepository;
    @Mock private MasterPlaylistKeyRepository masterPlaylistKeyRepository;
    @Mock private HistoryProgressClient historyProgressClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private OutboxService outboxService;

    private PlaybackUrlSigner urlSigner;
    private StreamingService streamingService;

    private final UUID userId = UUID.randomUUID();
    private final UUID titleId = UUID.randomUUID();
    private final UUID episodeId = UUID.randomUUID();
    private final String playlistKey = "titles/" + titleId + "/hls/master.m3u8";

    @BeforeEach
    void setUp() {
        urlSigner = new PlaybackUrlSigner(CDN, SECRET, mockProvider());
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        streamingService = new StreamingService(
                playbackSessionRepository,
                availabilityRepository,
                masterPlaylistKeyRepository,
                urlSigner,
                historyProgressClient,
                new TransactionTemplate(txManager),
                new PlaybackMetrics(new SimpleMeterRegistry()),
                redisTemplate,
                outboxService);
    }

    @Test
    @DisplayName("createSession returns a signed CDN manifest URL, stores the key (not the URL), and emits playback.started")
    void createSession_Success() {
        publishedTitle(titleId, null);
        when(masterPlaylistKeyRepository.findById(titleId)).thenReturn(Optional.of(keyRow()));
        when(playbackSessionRepository.countByUserIdAndEndedAtIsNullAndExpiresAtAfter(eq(userId), any())).thenReturn(0L);
        when(historyProgressClient.resumePositionSeconds(eq(titleId), anyString())).thenReturn(125);
        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenAnswer(i -> withId(i.getArgument(0)));

        PlaybackSessionResponse response = streamingService.createSession(
                userId, Constants.PLAN_FREE, "Bearer token", request(titleId, "MOVIE"), "127.0.0.1");

        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getResumePositionSeconds()).isEqualTo(125);
        assertThat(response.getManifestUrl())
                .startsWith(CDN + "/" + playlistKey + "?exp=")
                .contains("&sig=");
        assertThat(response.getExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<PlaybackSession> saved = ArgumentCaptor.forClass(PlaybackSession.class);
        verify(playbackSessionRepository).save(saved.capture());
        assertThat(saved.getValue().getMasterPlaylistUrl()).isEqualTo(playlistKey);
        assertThat(saved.getValue().getResumePositionSeconds()).isEqualTo(125);
        verify(outboxService).save(any());
    }

    @Test
    @DisplayName("createSession rejects titles that are unknown or unpublished with 404 (no draft enumeration)")
    void createSession_UnpublishedIsNotFound() {
        when(availabilityRepository.findById(titleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamingService.createSession(userId, Constants.PLAN_FREE, null,
                request(titleId, "MOVIE"), "127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TITLE_NOT_FOUND));
        verify(playbackSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSession for an episode requires the episode and its series to be published")
    void createSession_EpisodeRequiresPublishedSeries() {
        when(availabilityRepository.findById(episodeId)).thenReturn(Optional.of(
                TitleAvailability.builder().contentId(episodeId).parentId(titleId).published(true).build()));
        when(availabilityRepository.findById(titleId)).thenReturn(Optional.of(
                TitleAvailability.builder().contentId(titleId).published(false).build()));

        assertThatThrownBy(() -> streamingService.createSession(userId, Constants.PLAN_FREE, null,
                request(episodeId, "EPISODE"), "127.0.0.1"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("createSession refuses a MOVIE request for a series id")
    void createSession_MovieTargetMustNotHaveParent() {
        when(availabilityRepository.findById(titleId)).thenReturn(Optional.of(
                TitleAvailability.builder().contentId(titleId).parentId(UUID.randomUUID()).published(true).build()));

        assertThatThrownBy(() -> streamingService.createSession(userId, Constants.PLAN_FREE, null,
                request(titleId, "MOVIE"), "127.0.0.1"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("createSession returns ASSET_NOT_READY when no master playlist has been registered")
    void createSession_NoMasterKeyIsNotReady() {
        publishedTitle(titleId, null);
        when(masterPlaylistKeyRepository.findById(titleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamingService.createSession(userId, Constants.PLAN_FREE, null,
                request(titleId, "MOVIE"), "127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ASSET_NOT_READY));
    }

    @Test
    @DisplayName("FREE plan allows one concurrent stream; the second request is rejected with 429")
    void createSession_EnforcesPlanConcurrency() {
        publishedTitle(titleId, null);
        when(masterPlaylistKeyRepository.findById(titleId)).thenReturn(Optional.of(keyRow()));
        when(historyProgressClient.resumePositionSeconds(any(), any())).thenReturn(0);
        when(playbackSessionRepository.countByUserIdAndEndedAtIsNullAndExpiresAtAfter(eq(userId), any())).thenReturn(1L);

        assertThatThrownBy(() -> streamingService.createSession(userId, Constants.PLAN_FREE, null,
                request(titleId, "MOVIE"), "127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_STREAM_LIMIT));
        verify(playbackSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("PREMIUM allows four concurrent streams; an unknown plan is treated as FREE")
    void planLimits() {
        assertThat(StreamingService.normalizePlan("premium")).isEqualTo(Constants.PLAN_PREMIUM);
        assertThat(StreamingService.normalizePlan("gold")).isEqualTo(Constants.PLAN_FREE);
        assertThat(StreamingService.normalizePlan(null)).isEqualTo(Constants.PLAN_FREE);
        assertThat(StreamingService.CONCURRENT_STREAM_LIMITS.get(Constants.PLAN_PREMIUM)).isEqualTo(4);
    }

    @Test
    @DisplayName("createSession reads the master key from Redis when cached, without touching the database")
    void createSession_UsesRedisCache() {
        publishedTitle(titleId, null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:master-key:" + titleId)).thenReturn(playlistKey);
        when(historyProgressClient.resumePositionSeconds(any(), any())).thenReturn(0);
        when(playbackSessionRepository.countByUserIdAndEndedAtIsNullAndExpiresAtAfter(eq(userId), any())).thenReturn(0L);
        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenAnswer(i -> withId(i.getArgument(0)));

        streamingService.createSession(userId, Constants.PLAN_FREE, null, request(titleId, "MOVIE"), "127.0.0.1");

        verify(masterPlaylistKeyRepository, never()).findById(any());
    }

    @Test
    @DisplayName("endSession stamps ended_at once and emits playback.ended; a repeat call is a no-op")
    void endSession_IsIdempotent() {
        UUID sessionId = UUID.randomUUID();
        PlaybackSession open = PlaybackSession.builder()
                .id(sessionId).userId(userId).contentId(titleId).targetType("MOVIE")
                .masterPlaylistUrl(playlistKey).expiresAt(Instant.now().plusSeconds(600)).build();
        when(playbackSessionRepository.findByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(open))
                .thenReturn(Optional.of(open));

        streamingService.endSession(userId, sessionId);
        streamingService.endSession(userId, sessionId);

        assertThat(open.getEndedAt()).isNotNull();
        verify(playbackSessionRepository, org.mockito.Mockito.times(1)).save(open);
        verify(outboxService, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    @DisplayName("endSession returns SESSION_NOT_FOUND for another user's session")
    void endSession_OtherUserIsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(playbackSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamingService.endSession(userId, sessionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    @DisplayName("registerMasterPlaylistKey upserts the key and refreshes the Redis cache")
    void registerMasterPlaylistKey_UpsertsAndCaches() {
        when(masterPlaylistKeyRepository.findById(titleId)).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        streamingService.registerMasterPlaylistKey(titleId, playlistKey);

        ArgumentCaptor<MasterPlaylistKey> saved = ArgumentCaptor.forClass(MasterPlaylistKey.class);
        verify(masterPlaylistKeyRepository).save(saved.capture());
        assertThat(saved.getValue().getPlaylistKey()).isEqualTo(playlistKey);
        verify(valueOperations).set(eq("content:master-key:" + titleId), eq(playlistKey), anyLong(), any());
    }

    @Test
    @DisplayName("recordEpisodePublication links the episode to its series so it inherits the series' state")
    void recordEpisodePublication_LinksParent() {
        when(availabilityRepository.findById(episodeId)).thenReturn(Optional.empty());

        streamingService.recordEpisodePublication(episodeId, titleId);

        ArgumentCaptor<TitleAvailability> saved = ArgumentCaptor.forClass(TitleAvailability.class);
        verify(availabilityRepository).save(saved.capture());
        assertThat(saved.getValue().getParentId()).isEqualTo(titleId);
        assertThat(saved.getValue().isPublished()).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    private void publishedTitle(UUID id, UUID parent) {
        when(availabilityRepository.findById(id)).thenReturn(Optional.of(
                TitleAvailability.builder().contentId(id).parentId(parent).published(true).build()));
    }

    private MasterPlaylistKey keyRow() {
        return MasterPlaylistKey.builder().contentId(titleId).playlistKey(playlistKey).updatedAt(Instant.now()).build();
    }

    private static CreateSessionRequest request(UUID contentId, String targetType) {
        return CreateSessionRequest.builder().contentId(contentId).targetType(targetType).deviceId("device").build();
    }

    private static PlaybackSession withId(PlaybackSession session) {
        session.setId(UUID.randomUUID());
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(Instant.now());
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    private static org.springframework.beans.factory.ObjectProvider<com.vyroflix.common.storage.ObjectStorageClient> mockProvider() {
        return mock(org.springframework.beans.factory.ObjectProvider.class);
    }
}
