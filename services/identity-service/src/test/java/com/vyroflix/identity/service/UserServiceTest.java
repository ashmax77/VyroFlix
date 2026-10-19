package com.vyroflix.identity.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.identity.dto.SyncUserRequest;
import com.vyroflix.identity.dto.UpdateProfileRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.entity.User;
import com.vyroflix.identity.entity.UserProfile;
import com.vyroflix.identity.repository.UserProfileRepository;
import com.vyroflix.identity.repository.UserRepository;
import com.vyroflix.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private ObjectProvider<OutboxService> outboxServiceProvider;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UserService userService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("syncUser creates new user and profile if user does not exist")
    void syncUser_CreatesNewUser() {
        when(outboxServiceProvider.getIfAvailable()).thenReturn(outboxService);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);

        User savedUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .roles(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .displayName("John Doe")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();
        savedUser.setProfile(profile);
        savedUser.addRole("ROLE_USER");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        SyncUserRequest request = SyncUserRequest.builder()
                .userId(userId)
                .email("user@example.com")
                .displayName("John Doe")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        UserProfileResponse response = userService.syncUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getDisplayName()).isEqualTo("John Doe");
        assertThat(response.getRoles()).contains("ROLE_USER");

        verify(userRepository).save(any(User.class));
        verify(outboxService).save(any());
    }

    @Test
    @DisplayName("syncUser updates profile if user already exists")
    void syncUser_UpdatesExistingUser() {
        User existingUser = User.builder()
                .id(userId)
                .email("existing@example.com")
                .roles(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserProfile profile = UserProfile.builder()
                .user(existingUser)
                .displayName("Old Name")
                .avatarUrl("https://old.com/avatar.jpg")
                .build();
        existingUser.setProfile(profile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        SyncUserRequest request = SyncUserRequest.builder()
                .userId(userId)
                .email("existing@example.com")
                .displayName("New Name")
                .avatarUrl("https://new.com/avatar.jpg")
                .build();

        UserProfileResponse response = userService.syncUser(request);

        assertThat(response).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo("New Name");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://new.com/avatar.jpg");
        verify(userProfileRepository).save(profile);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("syncUser throws CONFLICT if email already registered with different user ID")
    void syncUser_ThrowsConflictOnDuplicateEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        SyncUserRequest request = SyncUserRequest.builder()
                .userId(userId)
                .email("taken@example.com")
                .build();

        assertThatThrownBy(() -> userService.syncUser(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("getProfile returns profile for existing user")
    void getProfile_ReturnsProfile() {
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .roles(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .displayName("Test User")
                .build();
        user.setProfile(profile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("getProfile throws NOT_FOUND when user does not exist")
    void getProfile_ThrowsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("updateProfile updates and saves profile fields")
    void updateProfile_UpdatesFields() {
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .roles(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .displayName("Before")
                .avatarUrl("https://before.com")
                .build();
        user.setProfile(profile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName("After")
                .avatarUrl("https://after.com")
                .build();

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo("After");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://after.com");
        verify(userProfileRepository).save(profile);
    }
}
