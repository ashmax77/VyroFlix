package com.vyroflix.identity.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.util.Constants;
import com.vyroflix.identity.dto.SyncUserRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<UserProfileResponse> syncUser(
            @RequestHeader(value = Constants.HEADER_USER_ID, required = false) String headerUserId,
            @Valid @RequestBody SyncUserRequest request) {

        if (headerUserId != null && !headerUserId.isBlank()) {
            UUID authenticatedUserId = UUID.fromString(headerUserId);
            if (!authenticatedUserId.equals(request.getUserId())) {
                log.warn("Header user ID [{}] does not match request user ID [{}]", authenticatedUserId, request.getUserId());
                throw new ApiException(ErrorCode.FORBIDDEN, "User ID does not match authenticated user");
            }
        }

        UserProfileResponse response = userService.syncUser(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
