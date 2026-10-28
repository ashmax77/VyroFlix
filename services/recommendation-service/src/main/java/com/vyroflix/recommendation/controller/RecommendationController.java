package com.vyroflix.recommendation.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.recommendation.dto.RecommendationRailResponse;
import com.vyroflix.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        if (principal == null || principal.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required for personal recommendations");
        }
        UUID userId = principal.getUserId();
        log.debug("Fetching personal recommendations for userId={}, limit={}", userId, limit);
        RecommendationRailResponse rail = recommendationService.getPersonalRecommendations(userId, limit);
        return ResponseEntity.ok(rail);
    }
}
