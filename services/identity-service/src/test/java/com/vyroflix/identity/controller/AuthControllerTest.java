package com.vyroflix.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.util.Constants;
import com.vyroflix.identity.dto.SyncUserRequest;
import com.vyroflix.identity.dto.UserProfileResponse;
import com.vyroflix.identity.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/auth/sync returns 200 with synced profile")
    void syncUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        SyncUserRequest request = SyncUserRequest.builder()
                .userId(userId)
                .email("sync@example.com")
                .displayName("Sync User")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("sync@example.com")
                .displayName("Sync User")
                .avatarUrl("https://example.com/avatar.jpg")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.syncUser(any(SyncUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.HEADER_USER_ID, userId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("sync@example.com"))
                .andExpect(jsonPath("$.displayName").value("Sync User"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/sync returns 403 when header user ID does not match body user ID")
    void syncUser_ForbiddenOnMismatch() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        SyncUserRequest request = SyncUserRequest.builder()
                .userId(userId)
                .email("sync@example.com")
                .displayName("Sync User")
                .build();

        mockMvc.perform(post("/api/v1/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.HEADER_USER_ID, otherUserId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sync returns 400 when validation fails")
    void syncUser_ValidationFails() throws Exception {
        SyncUserRequest request = SyncUserRequest.builder()
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/v1/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
