package com.vyroflix.recommendation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationConsumerTest {

    @Mock
    private RecommendationService recommendationService;

    private ObjectMapper objectMapper;
    private CatalogPublishedConsumer catalogPublishedConsumer;
    private PlaybackStartedConsumer playbackStartedConsumer;
    private ContentCompletedConsumer contentCompletedConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        catalogPublishedConsumer = new CatalogPublishedConsumer(recommendationService, objectMapper);
        playbackStartedConsumer = new PlaybackStartedConsumer(recommendationService, objectMapper);
        contentCompletedConsumer = new ContentCompletedConsumer(recommendationService, objectMapper);
    }

    @Test
    @DisplayName("CatalogPublishedConsumer indexes title metadata")
    void testCatalogPublishedConsumer() throws Exception {
        UUID titleId = UUID.randomUUID();
        TitlePublishedEventPayload payload = TitlePublishedEventPayload.builder()
                .titleId(titleId)
                .title("Dune")
                .type("MOVIE")
                .genreSlugs(Set.of("sci-fi"))
                .build();

        EventEnvelope<TitlePublishedEventPayload> envelope = EventEnvelope.of(
                "catalog.title.published.v1",
                "content-service",
                UUID.randomUUID(),
                payload
        );

        catalogPublishedConsumer.consume(objectMapper.writeValueAsString(envelope));
        verify(recommendationService).indexTitleMetadata(any());
    }

    @Test
    @DisplayName("PlaybackStartedConsumer records playback start")
    void testPlaybackStartedConsumer() throws Exception {
        UUID contentId = UUID.randomUUID();
        Map<String, Object> envelope = Map.of(
                "eventType", "playback.started.v1",
                "payload", Map.of("contentId", contentId.toString())
        );

        playbackStartedConsumer.consume(objectMapper.writeValueAsString(envelope));
        verify(recommendationService).recordPlaybackStarted(eq(contentId));
    }

    @Test
    @DisplayName("ContentCompletedConsumer records viewing progress completion")
    void testContentCompletedConsumer() throws Exception {
        UUID contentId = UUID.randomUUID();
        Map<String, Object> envelope = Map.of(
                "eventType", "history.content.completed.v1",
                "payload", Map.of("contentId", contentId.toString())
        );

        contentCompletedConsumer.consume(objectMapper.writeValueAsString(envelope));
        verify(recommendationService).recordViewingProgress(eq(contentId), eq(true));
    }
}
