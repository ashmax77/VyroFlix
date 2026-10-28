package com.vyroflix.identity.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.identity.dto.SyncUserRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<UserProfileResponse> syncUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SyncUserRequest request) {

        if (principal == null || principal.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to sync a user profile");
        }
        if (!principal.getUserId().equals(request.getUserId())) {
            log.warn("Authenticated user [{}] attempted to sync a different user [{}]", principal.getUserId(), request.getUserId());
            throw new ApiException(ErrorCode.FORBIDDEN, "User ID does not match authenticated user");
        }

        UserProfileResponse response = userService.syncUser(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
