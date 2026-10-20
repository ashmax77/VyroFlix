package com.vyroflix.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.recommendation.dto.RecommendationItemResponse;
import com.vyroflix.recommendation.dto.RecommendationRailResponse;
import com.vyroflix.recommendation.entity.RecommendationCache;
import com.vyroflix.recommendation.entity.TitleMetadata;
import com.vyroflix.recommendation.entity.TrendingScore;
import com.vyroflix.recommendation.repository.RecommendationCacheRepository;
import com.vyroflix.recommendation.repository.TitleMetadataRepository;
import com.vyroflix.recommendation.repository.TrendingScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final TrendingScoreRepository trendingScoreRepository;
    private final TitleMetadataRepository titleMetadataRepository;
    private final RecommendationCacheRepository recommendationCacheRepository;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(2);

    /**
     * Retrieves the top trending titles based on viewing and completion velocity.
     * Fails open with fallback metadata on any issue.
     */
    @Transactional(readOnly = true)
    public RecommendationRailResponse getTrendingRail(int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 30);
        try {
            List<TrendingScore> scores = trendingScoreRepository.findAllByOrderByScoreDesc(PageRequest.of(0, boundedLimit));

            if (!scores.isEmpty()) {
                Map<UUID, TitleMetadata> metaMap = titleMetadataRepository.findByTitleIdIn(
                        scores.stream().map(TrendingScore::getTitleId).collect(Collectors.toList())
                ).stream().collect(Collectors.toMap(TitleMetadata::getTitleId, m -> m));

                List<RecommendationItemResponse> items = new ArrayList<>();
                for (int i = 0; i < scores.size(); i++) {
                    TrendingScore score = scores.get(i);
                    TitleMetadata meta = metaMap.get(score.getTitleId());
                    if (meta != null) {
                        items.add(toItem(meta, score.getScore(), "Trending #" + (i + 1)));
                    }
                }

                if (!items.isEmpty()) {
                    return RecommendationRailResponse.builder()
                            .railType("TRENDING_NOW")
                            .title("Trending Now")
                            .items(items)
                            .build();
                }
            }

            // Fallback: return top published titles from metadata
            List<TitleMetadata> fallback = titleMetadataRepository.findAll(PageRequest.of(0, boundedLimit)).getContent();
            List<RecommendationItemResponse> items = fallback.stream()
                    .map(m -> toItem(m, 1.0, "Popular on VyroFlix"))
                    .collect(Collectors.toList());

            return RecommendationRailResponse.builder()
                    .railType("TRENDING_NOW")
                    .title("Trending Now")
                    .items(items)
                    .build();

        } catch (Exception e) {
            log.error("Fail-open: trending calculation encountered an error, returning empty rail", e);
            return RecommendationRailResponse.builder()
                    .railType("TRENDING_NOW")
                    .title("Trending Now")
                    .items(List.of())
                    .build();
        }
    }

    /**
     * Calculates content-based similarity between titles using the Jaccard index on genre sets:
     * Jaccard(A, B) = |genres(A) ∩ genres(B)| / |genres(A) ∪ genres(B)|
     */
    @Transactional(readOnly = true)
    public RecommendationRailResponse getSimilarTitles(UUID targetTitleId, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        try {
            Optional<TitleMetadata> targetOpt = titleMetadataRepository.findByTitleId(targetTitleId);
            if (targetOpt.isEmpty()) {
                return RecommendationRailResponse.builder()
                        .railType("SIMILAR_TITLES")
                        .title("More Like This")
                        .items(List.of())
                        .build();
            }

            TitleMetadata target = targetOpt.get();
            Set<String> targetGenres = new HashSet<>(target.getGenres() != null ? target.getGenres() : List.of());

            List<TitleMetadata> candidates = titleMetadataRepository.findAll().stream()
                    .filter(c -> !c.getTitleId().equals(targetTitleId))
                    .collect(Collectors.toList());

            // Compute Jaccard similarity and rank
            List<RecommendationItemResponse> ranked = candidates.stream()
                    .map(candidate -> {
                        Set<String> candidateGenres = new HashSet<>(candidate.getGenres() != null ? candidate.getGenres() : List.of());
                        double similarity = computeJaccardSimilarity(targetGenres, candidateGenres);
                        return new AbstractMap.SimpleEntry<>(candidate, similarity);
                    })
                    .filter(entry -> entry.getValue() > 0.0)
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(boundedLimit)
                    .map(entry -> toItem(entry.getKey(), entry.getValue(), "Similar genres: " + String.join(", ", entry.getKey().getGenres())))
                    .collect(Collectors.toList());

            return RecommendationRailResponse.builder()
                    .railType("SIMILAR_TITLES")
                    .title("More Like This")
                    .items(ranked)
                    .build();

        } catch (Exception e) {
            log.error("Fail-open: similarity calculation failed for targetId={}", targetTitleId, e);
            return RecommendationRailResponse.builder()
                    .railType("SIMILAR_TITLES")
                    .title("More Like This")
                    .items(List.of())
                    .build();
        }
    }

    /**
     * Retrieves personalized recommendations for an authenticated user, leveraging cache with 2-hour TTL.
     */
    @Transactional
    public RecommendationRailResponse getPersonalRecommendations(UUID userId, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        try {
            // Check cache
            Optional<RecommendationCache> cached = recommendationCacheRepository
                    .findByUserIdAndRailTypeAndExpiresAtAfter(userId, "PERSONAL", Instant.now());

            if (cached.isPresent()) {
                try {
                    List<UUID> titleIds = objectMapper.readValue(
                            cached.get().getTitleIdsJson(),
                            new TypeReference<List<UUID>>() {}
                    );
                    Map<UUID, TitleMetadata> metaMap = titleMetadataRepository.findByTitleIdIn(titleIds)
                            .stream().collect(Collectors.toMap(TitleMetadata::getTitleId, m -> m));

                    List<RecommendationItemResponse> items = titleIds.stream()
                            .map(metaMap::get)
                            .filter(Objects::nonNull)
                            .map(m -> toItem(m, 0.95, "Recommended for You"))
                            .limit(boundedLimit)
                            .collect(Collectors.toList());

                    return RecommendationRailResponse.builder()
                            .railType("PERSONAL")
                            .title("Recommended for You")
                            .items(items)
                            .build();
                } catch (Exception e) {
                    log.warn("Corrupted cache entry for user {}, recomputing", userId);
                }
            }

            // Fresh calculation: use trending titles enriched as personal recommendations
            RecommendationRailResponse trending = getTrendingRail(boundedLimit);
            List<RecommendationItemResponse> items = trending.getItems();

            // Cache computed title IDs
            List<UUID> titleIdsToCache = items.stream().map(RecommendationItemResponse::getTitleId).collect(Collectors.toList());
            RecommendationCache cacheEntry = recommendationCacheRepository.findByUserIdAndRailType(userId, "PERSONAL")
                    .orElse(RecommendationCache.builder()
                            .userId(userId)
                            .railType("PERSONAL")
                            .build());

            cacheEntry.setTitleIdsJson(objectMapper.writeValueAsString(titleIdsToCache));
            cacheEntry.setExpiresAt(Instant.now().plus(CACHE_TTL));
            recommendationCacheRepository.save(cacheEntry);

            return RecommendationRailResponse.builder()
                    .railType("PERSONAL")
                    .title("Recommended for You")
                    .items(items)
                    .build();

        } catch (Exception e) {
            log.error("Fail-open: personal recommendation calculation failed for userId={}", userId, e);
            return RecommendationRailResponse.builder()
                    .railType("PERSONAL")
                    .title("Recommended for You")
                    .items(List.of())
                    .build();
        }
    }

    /**
     * Increments viewing metrics when playback starts.
     */
    @Transactional
    public void recordPlaybackStarted(UUID titleId) {
        if (titleId == null) return;
        TrendingScore score = trendingScoreRepository.findByTitleId(titleId)
                .orElse(TrendingScore.builder()
                        .titleId(titleId)
                        .calculatedAt(Instant.now())
                        .build());

        score.setViewCount(score.getViewCount() + 1);
        score.setScore(computeScore(score.getViewCount(), score.getCompletionCount()));
        score.setCalculatedAt(Instant.now());
        trendingScoreRepository.save(score);
    }

    /**
     * Updates completion metrics when viewing progress indicates completion.
     */
    @Transactional
    public void recordViewingProgress(UUID titleId, boolean completed) {
        if (titleId == null || !completed) return;
        TrendingScore score = trendingScoreRepository.findByTitleId(titleId)
                .orElse(TrendingScore.builder()
                        .titleId(titleId)
                        .calculatedAt(Instant.now())
                        .build());

        score.setCompletionCount(score.getCompletionCount() + 1);
        score.setScore(computeScore(score.getViewCount(), score.getCompletionCount()));
        score.setCalculatedAt(Instant.now());
        trendingScoreRepository.save(score);
    }

    /**
     * Indexes or updates title metadata when published.
     */
    @Transactional
    public void indexTitleMetadata(TitlePublishedEventPayload payload) {
        if (payload == null || payload.getTitleId() == null) return;

        TitleMetadata meta = titleMetadataRepository.findByTitleId(payload.getTitleId())
                .orElse(TitleMetadata.builder()
                        .titleId(payload.getTitleId())
                        .createdAt(Instant.now())
                        .build());

        meta.setTitle(payload.getTitle());
        meta.setType(payload.getType() != null ? payload.getType() : "MOVIE");
        meta.setGenres(payload.getGenreSlugs() != null ? new ArrayList<>(payload.getGenreSlugs()) : List.of());
        meta.setPosterUrl(payload.getPosterUrl());
        meta.setReleaseYear(payload.getReleaseYear());
        meta.setMaturityRating(payload.getMaturityRating());
        meta.setUpdatedAt(Instant.now());

        titleMetadataRepository.save(meta);
    }

    private double computeScore(int views, int completions) {
        // Weighted formula: views * 1.0 + completions * 3.0
        return (views * 1.0) + (completions * 3.0);
    }

    private double computeJaccardSimilarity(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / (double) union.size();
    }

    private RecommendationItemResponse toItem(TitleMetadata meta, double score, String reason) {
        return RecommendationItemResponse.builder()
                .titleId(meta.getTitleId())
                .title(meta.getTitle())
                .type(meta.getType())
                .genres(meta.getGenres())
                .posterUrl(meta.getPosterUrl())
                .releaseYear(meta.getReleaseYear())
                .maturityRating(meta.getMaturityRating())
                .score(score)
                .reason(reason)
                .build();
    }
}
