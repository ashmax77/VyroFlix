package com.vyroflix.streaming.controller;

import com.vyroflix.streaming.dto.PlaybackStatsResponse;
import com.vyroflix.streaming.service.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only playback statistics (/api/v1/admin/** is restricted by the security chain). */
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StreamingService streamingService;

    @GetMapping("/playback")
    public ResponseEntity<PlaybackStatsResponse> playback(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ResponseEntity.ok(streamingService.playbackStats1(days));
    }
}
