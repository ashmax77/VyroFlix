package com.vyroflix.identity.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.identity.dto.UpdateProfileRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = resolveUserId(principal);
        UserProfileResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = resolveUserId(principal);
        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        UUID callerId = resolveUserId(principal);
        if (!callerId.equals(id) && !principal.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You may only view your own profile");
        }
        UserProfileResponse response = userService.getProfile(id);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(UserPrincipal principal) {
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to access user profile");
    }
}
