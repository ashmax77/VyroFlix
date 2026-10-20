package com.vyroflix.recommendation.controller;

import com.vyroflix.common.util.Constants;
import com.vyroflix.recommendation.dto.RecommendationRailResponse;
import com.vyroflix.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/trending")
    public ResponseEntity<RecommendationRailResponse> getTrending(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        log.debug("Fetching trending rail, limit={}", limit);
        RecommendationRailResponse rail = recommendationService.getTrendingRail(limit);
        return ResponseEntity.ok(rail);
    }

    @GetMapping("/similar/{titleId}")
    public ResponseEntity<RecommendationRailResponse> getSimilar(
            @PathVariable("titleId") UUID titleId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        log.debug("Fetching similar titles for titleId={}, limit={}", titleId, limit);
        RecommendationRailResponse rail = recommendationService.getSimilarTitles(titleId, limit);
        return ResponseEntity.ok(rail);
    }

    @GetMapping("/personal")
    public ResponseEntity<RecommendationRailResponse> getPersonal(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        UUID userId = resolveUserId(headerUserId);
        log.debug("Fetching personal recommendations for userId={}, limit={}", userId, limit);
        RecommendationRailResponse rail = recommendationService.getPersonalRecommendations(userId, limit);
        return ResponseEntity.ok(rail);
    }

    private UUID resolveUserId(String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return UUID.fromString(headerUserId.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        // Fallback demo user ID
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
