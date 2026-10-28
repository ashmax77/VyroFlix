package com.vyroflix.recommendation.service;

import com.vyroflix.recommendation.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationEventProcessorTest {

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private RecommendationEventProcessor processor;

    @Test
    @DisplayName("first delivery of an event updates the counters")
    void firstDeliveryIsApplied() {
        UUID titleId = UUID.randomUUID();
        when(processedEventRepository.claim("evt-1", RecommendationEventProcessor.CONSUMER_PLAYBACK_STARTED)).thenReturn(1);

        processor.onPlaybackStarted("evt-1", titleId);

        verify(recommendationService).recordPlaybackStarted(titleId);
    }

    @Test
    @DisplayName("a redelivered event is skipped and does not double count")
    void duplicateDeliveryIsSkipped() {
        UUID titleId = UUID.randomUUID();
        when(processedEventRepository.claim("evt-1", RecommendationEventProcessor.CONSUMER_CONTENT_COMPLETED))
                .thenReturn(1)
                .thenReturn(0);

        processor.onContentCompleted("evt-1", titleId);
        processor.onContentCompleted("evt-1", titleId);

        verify(recommendationService, times(1)).recordViewingProgress(titleId, true);
    }

    @Test
    @DisplayName("events without an id cannot be deduplicated and are applied")
    void eventsWithoutIdAreApplied() {
        UUID titleId = UUID.randomUUID();

        processor.onPlaybackStarted(null, titleId);

        verify(processedEventRepository, never()).claim(anyString(), anyString());
        verify(recommendationService).recordPlaybackStarted(titleId);
    }
}
