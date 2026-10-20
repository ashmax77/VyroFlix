package com.vyroflix.history.service;

import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.history.dto.*;
import com.vyroflix.history.entity.CompletionRecord;
import com.vyroflix.history.entity.TargetType;
import com.vyroflix.history.entity.ViewingProgress;
import com.vyroflix.history.entity.WatchlistEntry;
import com.vyroflix.history.repository.CompletionRecordRepository;
import com.vyroflix.history.repository.ViewingProgressRepository;
import com.vyroflix.history.repository.WatchlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private ViewingProgressRepository viewingProgressRepository;

    @Mock
    private WatchlistEntryRepository watchlistEntryRepository;

    @Mock
    private CompletionRecordRepository completionRecordRepository;

    @Mock
    private OutboxService outboxService;

    private HistoryService historyService;

    private UUID userId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        historyService = new HistoryService(
                viewingProgressRepository,
                watchlistEntryRepository,
                completionRecordRepository,
                outboxService
        );
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("recordProgress creates new viewing progress when under 90% threshold")
    void recordProgress_UnderThreshold_CreatesNewProgress() {
        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(300)
                .durationSeconds(1000) // 30%
                .build();

        when(viewingProgressRepository.findByUserIdAndContentId(userId, contentId)).thenReturn(Optional.empty());
        when(viewingProgressRepository.save(any(ViewingProgress.class))).thenAnswer(inv -> {
            ViewingProgress vp = inv.getArgument(0);
            vp.setId(UUID.randomUUID());
            return vp;
        });

        ViewingProgressResponse response = historyService.recordProgress(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getContentId()).isEqualTo(contentId);
        assertThat(response.getPositionSeconds()).isEqualTo(300);
        assertThat(response.getProgressPercent()).isEqualTo(30.0);
        assertThat(response.isCompleted()).isFalse();

        verify(completionRecordRepository, never()).save(any());
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("recordProgress marks content as completed and publishes outbox event when >= 90%")
    void recordProgress_AboveThreshold_MarksCompletedAndEmitsEvent() {
        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(910)
                .durationSeconds(1000) // 91%
                .build();

        when(viewingProgressRepository.findByUserIdAndContentId(userId, contentId)).thenReturn(Optional.empty());
        when(viewingProgressRepository.save(any(ViewingProgress.class))).thenAnswer(inv -> {
            ViewingProgress vp = inv.getArgument(0);
            vp.setId(UUID.randomUUID());
            return vp;
        });
        when(completionRecordRepository.existsByUserIdAndContentId(userId, contentId)).thenReturn(false);

        ViewingProgressResponse response = historyService.recordProgress(userId, request);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.getProgressPercent()).isEqualTo(91.0);

        verify(completionRecordRepository).save(any(CompletionRecord.class));

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getEventType()).isEqualTo(Constants.TOPIC_HISTORY_CONTENT_COMPLETED);
        assertThat(envelopeCaptor.getValue().getProducer()).isEqualTo(Constants.SERVICE_HISTORY);
    }

    @Test
    @DisplayName("recordProgress does not duplicate completion record if already marked complete")
    void recordProgress_AlreadyCompleted_DoesNotDuplicate() {
        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(950)
                .durationSeconds(1000)
                .build();

        ViewingProgress existing = ViewingProgress.builder()
                .userId(userId)
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(910)
                .durationSeconds(1000)
                .progressPercent(91.0)
                .isCompleted(true)
                .lastWatchedAt(Instant.now().minusSeconds(60))
                .build();
        existing.setId(UUID.randomUUID());

        when(viewingProgressRepository.findByUserIdAndContentId(userId, contentId)).thenReturn(Optional.of(existing));
        when(viewingProgressRepository.save(any(ViewingProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(completionRecordRepository.existsByUserIdAndContentId(userId, contentId)).thenReturn(true);

        ViewingProgressResponse response = historyService.recordProgress(userId, request);

        assertThat(response.isCompleted()).isTrue();
        verify(completionRecordRepository, never()).save(any());
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("getContinueWatching returns uncompleted viewing progress")
    void getContinueWatching_Success() {
        ViewingProgress vp = ViewingProgress.builder()
                .userId(userId)
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(500)
                .durationSeconds(1000)
                .progressPercent(50.0)
                .isCompleted(false)
                .lastWatchedAt(Instant.now())
                .build();
        vp.setId(UUID.randomUUID());

        when(viewingProgressRepository.findByUserIdAndIsCompletedFalseOrderByLastWatchedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(vp));

        List<ViewingProgressResponse> result = historyService.getContinueWatching(userId, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContentId()).isEqualTo(contentId);
        assertThat(result.get(0).getPositionSeconds()).isEqualTo(500);
    }

    @Test
    @DisplayName("addToWatchlist saves entry and publishes outbox event")
    void addToWatchlist_Success() {
        UUID titleId = UUID.randomUUID();
        AddToWatchlistRequest request = new AddToWatchlistRequest(titleId);

        when(watchlistEntryRepository.findByUserIdAndTitleId(userId, titleId)).thenReturn(Optional.empty());
        when(watchlistEntryRepository.save(any(WatchlistEntry.class))).thenAnswer(inv -> {
            WatchlistEntry entry = inv.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });

        WatchlistEntryResponse response = historyService.addToWatchlist(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitleId()).isEqualTo(titleId);

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getEventType()).isEqualTo(Constants.TOPIC_WATCHLIST_ITEM_ADDED);
    }

    @Test
    @DisplayName("addToWatchlist returns existing entry without re-emitting event")
    void addToWatchlist_AlreadyExists_ReturnsExisting() {
        UUID titleId = UUID.randomUUID();
        AddToWatchlistRequest request = new AddToWatchlistRequest(titleId);

        WatchlistEntry existing = WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .titleId(titleId)
                .createdAt(Instant.now())
                .build();

        when(watchlistEntryRepository.findByUserIdAndTitleId(userId, titleId)).thenReturn(Optional.of(existing));

        WatchlistEntryResponse response = historyService.addToWatchlist(userId, request);

        assertThat(response.getId()).isEqualTo(existing.getId());
        verify(watchlistEntryRepository, never()).save(any());
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("removeFromWatchlist deletes entry and publishes outbox event")
    void removeFromWatchlist_Success() {
        UUID titleId = UUID.randomUUID();
        WatchlistEntry existing = WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .titleId(titleId)
                .createdAt(Instant.now())
                .build();

        when(watchlistEntryRepository.findByUserIdAndTitleId(userId, titleId)).thenReturn(Optional.of(existing));

        historyService.removeFromWatchlist(userId, titleId);

        verify(watchlistEntryRepository).delete(existing);

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getEventType()).isEqualTo(Constants.TOPIC_WATCHLIST_ITEM_REMOVED);
    }

    @Test
    @DisplayName("isInWatchlist returns correct boolean status")
    void isInWatchlist_Success() {
        UUID titleId = UUID.randomUUID();
        when(watchlistEntryRepository.existsByUserIdAndTitleId(userId, titleId)).thenReturn(true);

        WatchlistStatusResponse status = historyService.isInWatchlist(userId, titleId);

        assertThat(status.isInWatchlist()).isTrue();
        assertThat(status.getTitleId()).isEqualTo(titleId);
    }
}
