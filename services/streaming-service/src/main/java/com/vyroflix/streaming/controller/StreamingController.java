package com.vyroflix.streaming.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.service.StreamingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/streaming")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;

    @PostMapping("/sessions")
    public ResponseEntity<PlaybackSessionResponse> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSessionRequest request,
            HttpServletRequest servletRequest) {

        UUID userId = resolveUserId(principal);
        String ipAddress = servletRequest.getRemoteAddr();

        PlaybackSessionResponse response = streamingService.createSession(userId, request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID resolveUserId(UserPrincipal principal) {
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to initiate playback session");
    }
}
