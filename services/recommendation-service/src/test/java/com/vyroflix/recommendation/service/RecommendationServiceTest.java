package com.vyroflix.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.recommendation.dto.RecommendationRailResponse;
import com.vyroflix.recommendation.entity.RecommendationCache;
import com.vyroflix.recommendation.entity.TitleMetadata;
import com.vyroflix.recommendation.entity.TrendingScore;
import com.vyroflix.recommendation.repository.RecommendationCacheRepository;
import com.vyroflix.recommendation.repository.TitleMetadataRepository;
import com.vyroflix.recommendation.repository.TrendingScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private TrendingScoreRepository trendingScoreRepository;

    @Mock
    private TitleMetadataRepository titleMetadataRepository;

    @Mock
    private RecommendationCacheRepository recommendationCacheRepository;

    private ObjectMapper objectMapper;
    private RecommendationService recommendationService;

    private UUID titleAId;
    private UUID titleBId;
    private UUID titleCId;
    private TitleMetadata titleA;
    private TitleMetadata titleB;
    private TitleMetadata titleC;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        recommendationService = new RecommendationService(
                trendingScoreRepository,
                titleMetadataRepository,
                recommendationCacheRepository,
                objectMapper
        );

        titleAId = UUID.randomUUID();
        titleBId = UUID.randomUUID();
        titleCId = UUID.randomUUID();

        titleA = TitleMetadata.builder()
                .titleId(titleAId)
                .title("Interstellar")
                .type("MOVIE")
                .genres(List.of("sci-fi", "space", "drama"))
                .releaseYear(2014)
                .build();

        titleB = TitleMetadata.builder()
                .titleId(titleBId)
                .title("Cosmic Odyssey")
                .type("MOVIE")
                .genres(List.of("sci-fi", "space", "adventure"))
                .releaseYear(2024)
                .build();

        titleC = TitleMetadata.builder()
                .titleId(titleCId)
                .title("Romantic Comedy")
                .type("MOVIE")
                .genres(List.of("romance", "comedy"))
                .releaseYear(2022)
                .build();
    }

    @Test
    @DisplayName("getTrendingRail() returns enriched trending rail from trending_scores")
    void getTrendingRailWithScores() {
        TrendingScore scoreA = TrendingScore.builder()
                .titleId(titleAId)
                .viewCount(100)
                .completionCount(50)
                .score(250.0)
                .build();

        when(trendingScoreRepository.findAllByOrderByScoreDesc(any(Pageable.class)))
                .thenReturn(List.of(scoreA));
        when(titleMetadataRepository.findByTitleIdIn(List.of(titleAId)))
                .thenReturn(List.of(titleA));

        RecommendationRailResponse rail = recommendationService.getTrendingRail(10);

        assertThat(rail.getItems()).hasSize(1);
        assertThat(rail.getItems().get(0).getTitle()).isEqualTo("Interstellar");
        assertThat(rail.getItems().get(0).getReason()).isEqualTo("Trending #1");
    }

    @Test
    @DisplayName("getSimilarTitles() computes Jaccard similarity between title genres")
    void getSimilarTitlesComputesJaccard() {
        when(titleMetadataRepository.findByTitleId(titleAId)).thenReturn(Optional.of(titleA));
        when(titleMetadataRepository.findAll()).thenReturn(List.of(titleA, titleB, titleC));

        // Interstellar (sci-fi, space, drama) vs Cosmic Odyssey (sci-fi, space, adventure):
        // Intersection = 2 (sci-fi, space), Union = 4 (sci-fi, space, drama, adventure) -> Jaccard = 0.5
        // Interstellar vs Romantic Comedy: Intersection = 0 -> Jaccard = 0.0 (excluded)
        RecommendationRailResponse rail = recommendationService.getSimilarTitles(titleAId, 5);

        assertThat(rail.getItems()).hasSize(1);
        assertThat(rail.getItems().get(0).getTitle()).isEqualTo("Cosmic Odyssey");
        assertThat(rail.getItems().get(0).getScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("getPersonalRecommendations() returns cached items when cache is fresh")
    void getPersonalRecommendationsFromCache() throws Exception {
        UUID userId = UUID.randomUUID();
        String json = objectMapper.writeValueAsString(List.of(titleAId));

        RecommendationCache cache = RecommendationCache.builder()
                .userId(userId)
                .railType("PERSONAL")
                .titleIdsJson(json)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(recommendationCacheRepository.findByUserIdAndRailTypeAndExpiresAtAfter(eq(userId), eq("PERSONAL"), any()))
                .thenReturn(Optional.of(cache));
        when(titleMetadataRepository.findByTitleIdIn(List.of(titleAId)))
                .thenReturn(List.of(titleA));

        RecommendationRailResponse rail = recommendationService.getPersonalRecommendations(userId, 5);

        assertThat(rail.getItems()).hasSize(1);
        assertThat(rail.getItems().get(0).getTitle()).isEqualTo("Interstellar");
        verify(recommendationCacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordPlaybackStarted() increments view count and recalculates score")
    void recordPlaybackStartedUpdatesScore() {
        when(trendingScoreRepository.findByTitleId(titleAId)).thenReturn(Optional.empty());

        recommendationService.recordPlaybackStarted(titleAId);

        ArgumentCaptor<TrendingScore> captor = ArgumentCaptor.forClass(TrendingScore.class);
        verify(trendingScoreRepository).save(captor.capture());

        TrendingScore saved = captor.getValue();
        assertThat(saved.getTitleId()).isEqualTo(titleAId);
        assertThat(saved.getViewCount()).isEqualTo(1);
        assertThat(saved.getScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordViewingProgress() increments completion count when completed")
    void recordViewingProgressUpdatesCompletion() {
        TrendingScore existing = TrendingScore.builder()
                .titleId(titleAId)
                .viewCount(10)
                .completionCount(2)
                .score(16.0)
                .build();

        when(trendingScoreRepository.findByTitleId(titleAId)).thenReturn(Optional.of(existing));

        recommendationService.recordViewingProgress(titleAId, true);

        ArgumentCaptor<TrendingScore> captor = ArgumentCaptor.forClass(TrendingScore.class);
        verify(trendingScoreRepository).save(captor.capture());

        TrendingScore saved = captor.getValue();
        assertThat(saved.getCompletionCount()).isEqualTo(3);
        // (10 * 1.0) + (3 * 3.0) = 19.0
        assertThat(saved.getScore()).isEqualTo(19.0);
    }

    @Test
    @DisplayName("indexTitleMetadata() upserts published title into recommendation metadata store")
    void indexTitleMetadataUpserts() {
        TitlePublishedEventPayload payload = TitlePublishedEventPayload.builder()
                .titleId(titleAId)
                .title("Interstellar")
                .type("MOVIE")
                .genreSlugs(Set.of("sci-fi", "drama"))
                .releaseYear(2014)
                .maturityRating("PG-13")
                .posterUrl("https://r2.vyroflix.com/poster.jpg")
                .build();

        when(titleMetadataRepository.findByTitleId(titleAId)).thenReturn(Optional.empty());

        recommendationService.indexTitleMetadata(payload);

        ArgumentCaptor<TitleMetadata> captor = ArgumentCaptor.forClass(TitleMetadata.class);
        verify(titleMetadataRepository).save(captor.capture());

        TitleMetadata saved = captor.getValue();
        assertThat(saved.getTitleId()).isEqualTo(titleAId);
        assertThat(saved.getTitle()).isEqualTo("Interstellar");
        assertThat(saved.getGenres()).containsExactlyInAnyOrder("sci-fi", "drama");
    }
}
