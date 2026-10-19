package com.vyroflix.streaming.service;

import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.entity.PlaybackSession;
import com.vyroflix.streaming.repository.PlaybackSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {

    @Mock
    private PlaybackSessionRepository playbackSessionRepository;

    @Mock
    private ObjectStorageClient storageClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OutboxService outboxService;

    private StreamingService streamingService;

    private UUID userId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createSession generates presigned URL, saves session, and publishes outbox event")
    void createSession_Success() {
        streamingService = new StreamingService(playbackSessionRepository, storageClient, null, outboxService);

        String expectedPlaylistKey = "hls/titles/" + contentId + "/master.m3u8";
        String presignedUrl = "https://cdn.vyroflix.com/vyroflix-content/" + expectedPlaylistKey + "?signature=test";

        when(storageClient.generatePresignedDownloadUrl(eq(Constants.BUCKET_CONTENT), eq(expectedPlaylistKey), any(Duration.class)))
                .thenReturn(presignedUrl);

        UUID sessionId = UUID.randomUUID();
        PlaybackSession savedSession = PlaybackSession.builder()
                .id(sessionId)
                .userId(userId)
                .contentId(contentId)
                .targetType("MOVIE")
                .masterPlaylistUrl(presignedUrl)
                .resumePositionSeconds(0)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenReturn(savedSession);

        CreateSessionRequest request = CreateSessionRequest.builder()
                .contentId(contentId)
                .targetType("MOVIE")
                .deviceId("device-browser-1")
                .build();

        PlaybackSessionResponse response = streamingService.createSession(userId, request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getManifestUrl()).isEqualTo(presignedUrl);
        assertThat(response.getResumePositionSeconds()).isEqualTo(0);

        verify(playbackSessionRepository).save(any(PlaybackSession.class));
        verify(outboxService).save(any());
    }

    @Test
    @DisplayName("createSession uses cached playlist key from Redis if present")
    void createSession_UsesRedisCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        streamingService = new StreamingService(playbackSessionRepository, storageClient, redisTemplate, outboxService);

        String cachedKey = "hls/custom/master.m3u8";
        when(valueOperations.get("content:master-key:" + contentId)).thenReturn(cachedKey);

        String presignedUrl = "https://cdn.vyroflix.com/" + cachedKey + "?token=123";
        when(storageClient.generatePresignedDownloadUrl(eq(Constants.BUCKET_CONTENT), eq(cachedKey), any(Duration.class)))
                .thenReturn(presignedUrl);

        UUID sessionId = UUID.randomUUID();
        PlaybackSession savedSession = PlaybackSession.builder()
                .id(sessionId)
                .userId(userId)
                .contentId(contentId)
                .targetType("MOVIE")
                .masterPlaylistUrl(presignedUrl)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenReturn(savedSession);

        CreateSessionRequest request = CreateSessionRequest.builder()
                .contentId(contentId)
                .build();

        PlaybackSessionResponse response = streamingService.createSession(userId, request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getManifestUrl()).isEqualTo(presignedUrl);
        verify(valueOperations).get("content:master-key:" + contentId);
    }
}
