package com.vyroflix.history.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.history.dto.*;
import com.vyroflix.history.service.HistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @PutMapping("/progress")
    public ResponseEntity<ViewingProgressResponse> recordProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecordProgressRequest request) {

        UUID userId = resolveUserId(principal);
        ViewingProgressResponse response = historyService.recordProgress(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress/{contentId}")
    public ResponseEntity<ViewingProgressResponse> getProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("contentId") UUID contentId) {

        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(historyService.getProgress(userId, contentId));
    }

    @GetMapping("/continue-watching")
    public ResponseEntity<List<ViewingProgressResponse>> getContinueWatching(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        UUID userId = resolveUserId(principal);
        List<ViewingProgressResponse> response = historyService.getContinueWatching(userId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistEntryResponse>> getWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "24") int size) {

        UUID userId = resolveUserId(principal);
        List<WatchlistEntryResponse> response = historyService.getWatchlist(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/watchlist")
    public ResponseEntity<WatchlistEntryResponse> addToWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToWatchlistRequest request) {

        UUID userId = resolveUserId(principal);
        WatchlistEntryResponse response = historyService.addToWatchlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/watchlist/{titleId}")
    public ResponseEntity<Void> removeFromWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("titleId") UUID titleId) {

        UUID userId = resolveUserId(principal);
        historyService.removeFromWatchlist(userId, titleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/watchlist/{titleId}/check")
    public ResponseEntity<WatchlistStatusResponse> checkWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("titleId") UUID titleId) {

        UUID userId = resolveUserId(principal);
        WatchlistStatusResponse response = historyService.isInWatchlist(userId, titleId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(UserPrincipal principal) {
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof UserPrincipal up && up.getUserId() != null) {
                return up.getUserId();
            }
            if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt && jwt.getSubject() != null) {
                try {
                    return UUID.fromString(jwt.getSubject());
                } catch (IllegalArgumentException ignored) {}
            }
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required for history operations");
    }
}
