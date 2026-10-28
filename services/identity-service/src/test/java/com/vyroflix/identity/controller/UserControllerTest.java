package com.vyroflix.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.identity.dto.UpdateProfileRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken authPrincipal(UUID userId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email("me@example.com")
                .role("USER")
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 200 with user profile")
    void getMe_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("me@example.com")
                .displayName("Me Myself")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.getProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Me Myself"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 401 when unauthenticated")
    void getMe_Unauthenticated() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me updates profile successfully")
    void updateMe_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName("Updated Name")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("me@example.com")
                .displayName("Updated Name")
                .avatarUrl("https://example.com/avatar.jpg")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(authentication(authPrincipal(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} returns 403 when a USER requests another user's profile")
    void getById_ForbiddenForOtherUser() throws Exception {
        UUID callerId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID())
                        .with(authentication(authPrincipal(callerId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} returns 200 when a USER requests their own profile")
    void getById_AllowedForSelf() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(UserProfileResponse.builder()
                .id(userId).email("me@example.com").roles(Set.of("ROLE_USER")).build());

        mockMvc.perform(get("/api/v1/users/" + userId)
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isOk());
    }
}
