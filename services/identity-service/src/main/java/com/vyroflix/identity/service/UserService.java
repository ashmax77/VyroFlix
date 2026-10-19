package com.vyroflix.identity.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.identity.dto.SyncUserRequest;
import com.vyroflix.identity.dto.UpdateProfileRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.entity.User;
import com.vyroflix.identity.entity.UserProfile;
import com.vyroflix.identity.entity.UserRole;
import com.vyroflix.identity.event.UserRegisteredPayload;
import com.vyroflix.identity.repository.UserProfileRepository;
import com.vyroflix.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectProvider<OutboxService> outboxServiceProvider;

    @Transactional
    public UserProfileResponse syncUser(SyncUserRequest request) {
        log.info("Synchronizing user with id={} email={}", request.getUserId(), request.getEmail());

        Optional<User> existingUserOpt = userRepository.findById(request.getUserId());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            UserProfile profile = existingUser.getProfile();
            if (profile != null) {
                if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                    profile.setDisplayName(request.getDisplayName());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                    profile.setAvatarUrl(request.getAvatarUrl());
                }
                userProfileRepository.save(profile);
            }
            return toResponse(existingUser);
        }

        // Check if email already used by a different user ID
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT, "A user with email " + request.getEmail() + " already exists");
        }

        // Create new User entity
        User newUser = User.builder()
                .id(request.getUserId())
                .email(request.getEmail())
                .isNew(true)
                .roles(new HashSet<>())
                .build();

        UserProfile newProfile = UserProfile.builder()
                .user(newUser)
                .displayName(request.getDisplayName())
                .avatarUrl(request.getAvatarUrl())
                .build();

        newUser.setProfile(newProfile);
        newUser.addRole("ROLE_" + Constants.ROLE_USER);

        User savedUser = userRepository.save(newUser);

        // Emit user.registered.v1 event via transactional outbox
        publishUserRegisteredEvent(savedUser);

        log.info("Successfully created and synchronized new user with id={}", savedUser.getId());
        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User profile not found for id: " + userId));
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User profile not found for id: " + userId));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder()
                    .user(user)
                    .displayName(request.getDisplayName())
                    .avatarUrl(request.getAvatarUrl())
                    .build();
            user.setProfile(profile);
        } else {
            if (request.getDisplayName() != null) {
                profile.setDisplayName(request.getDisplayName());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
        }

        userProfileRepository.save(profile);
        return toResponse(user);
    }

    private void publishUserRegisteredEvent(User user) {
        OutboxService outboxService = outboxServiceProvider.getIfAvailable();
        if (outboxService == null) {
            log.warn("OutboxService not available; skipping outbox event for user registration");
            return;
        }

        UserProfile profile = user.getProfile();
        String displayName = profile != null ? profile.getDisplayName() : null;

        UserRegisteredPayload payload = UserRegisteredPayload.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(displayName)
                .registeredAt(user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now())
                .build();

        EventEnvelope<UserRegisteredPayload> envelope = EventEnvelope.of(
                Constants.TOPIC_USER_REGISTERED,
                Constants.SERVICE_IDENTITY,
                UUID.randomUUID(),
                payload
        );

        outboxService.save(envelope);
    }

    private UserProfileResponse toResponse(User user) {
        UserProfile profile = user.getProfile();
        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet())
                : Set.of();

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
