package com.vyroflix.history.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.common.util.Constants;
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
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecordProgressRequest request) {

        UUID userId = resolveUserId(headerUserId, principal);
        ViewingProgressResponse response = historyService.recordProgress(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/continue-watching")
    public ResponseEntity<List<ViewingProgressResponse>> getContinueWatching(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        UUID userId = resolveUserId(headerUserId, principal);
        List<ViewingProgressResponse> response = historyService.getContinueWatching(userId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistEntryResponse>> getWatchlist(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "24") int size) {

        UUID userId = resolveUserId(headerUserId, principal);
        List<WatchlistEntryResponse> response = historyService.getWatchlist(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/watchlist")
    public ResponseEntity<WatchlistEntryResponse> addToWatchlist(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToWatchlistRequest request) {

        UUID userId = resolveUserId(headerUserId, principal);
        WatchlistEntryResponse response = historyService.addToWatchlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/watchlist/{titleId}")
    public ResponseEntity<Void> removeFromWatchlist(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("titleId") UUID titleId) {

        UUID userId = resolveUserId(headerUserId, principal);
        historyService.removeFromWatchlist(userId, titleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/watchlist/{titleId}/check")
    public ResponseEntity<WatchlistStatusResponse> checkWatchlist(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("titleId") UUID titleId) {

        UUID userId = resolveUserId(headerUserId, principal);
        WatchlistStatusResponse response = historyService.isInWatchlist(userId, titleId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(String headerUserId, UserPrincipal principal) {
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return UUID.fromString(headerUserId);
            } catch (IllegalArgumentException e) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid X-User-Id header format");
            }
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required for history operations");
    }
}
