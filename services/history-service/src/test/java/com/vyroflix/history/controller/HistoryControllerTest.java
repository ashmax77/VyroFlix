package com.vyroflix.history.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.history.dto.*;
import com.vyroflix.history.entity.TargetType;
import com.vyroflix.history.service.HistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HistoryService historyService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken authPrincipal(UUID userId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email("user@vyroflix.com")
                .role("USER")
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    @Test
    @DisplayName("PUT /api/v1/history/progress returns 200 OK with updated progress")
    void recordProgress_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(450)
                .durationSeconds(1000)
                .build();

        ViewingProgressResponse response = ViewingProgressResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(450)
                .durationSeconds(1000)
                .progressPercent(45.0)
                .completed(false)
                .lastWatchedAt(Instant.now())
                .build();

        when(historyService.recordProgress(eq(userId), any(RecordProgressRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/history/progress")
                        .with(authentication(authPrincipal(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.positionSeconds").value(450))
                .andExpect(jsonPath("$.progressPercent").value(45.0))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/history/progress without user authentication returns 401")
    void recordProgress_Unauthorized() throws Exception {
        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(UUID.randomUUID())
                .targetType(TargetType.MOVIE)
                .positionSeconds(100)
                .durationSeconds(1000)
                .build();

        mockMvc.perform(put("/api/v1/history/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/history/progress with invalid validation returns 400")
    void recordProgress_ValidationFailure() throws Exception {
        UUID userId = UUID.randomUUID();

        RecordProgressRequest request = RecordProgressRequest.builder()
                .contentId(null) // missing contentId
                .targetType(TargetType.MOVIE)
                .positionSeconds(-10) // invalid negative
                .durationSeconds(0) // invalid <= 0
                .build();

        mockMvc.perform(put("/api/v1/history/progress")
                        .with(authentication(authPrincipal(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/v1/history/continue-watching returns 200 OK with progress list")
    void getContinueWatching_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ViewingProgressResponse response = ViewingProgressResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .positionSeconds(300)
                .durationSeconds(900)
                .progressPercent(33.3)
                .completed(false)
                .lastWatchedAt(Instant.now())
                .build();

        when(historyService.getContinueWatching(eq(userId), eq(10))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/history/continue-watching")
                        .with(authentication(authPrincipal(userId)))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentId").value(contentId.toString()))
                .andExpect(jsonPath("$[0].positionSeconds").value(300));
    }

    @Test
    @DisplayName("GET /api/v1/history/watchlist returns 200 OK with watchlist entries")
    void getWatchlist_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();

        WatchlistEntryResponse response = WatchlistEntryResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .titleId(titleId)
                .createdAt(Instant.now())
                .build();

        when(historyService.getWatchlist(eq(userId), eq(0), eq(20))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/history/watchlist")
                        .with(authentication(authPrincipal(userId)))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titleId").value(titleId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/history/watchlist returns 201 Created")
    void addToWatchlist_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();
        AddToWatchlistRequest request = new AddToWatchlistRequest(titleId);

        WatchlistEntryResponse response = WatchlistEntryResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .titleId(titleId)
                .createdAt(Instant.now())
                .build();

        when(historyService.addToWatchlist(eq(userId), any(AddToWatchlistRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/history/watchlist")
                        .with(authentication(authPrincipal(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titleId").value(titleId.toString()));
    }

    @Test
    @DisplayName("DELETE /api/v1/history/watchlist/{titleId} returns 204 No Content")
    void removeFromWatchlist_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();

        doNothing().when(historyService).removeFromWatchlist(userId, titleId);

        mockMvc.perform(delete("/api/v1/history/watchlist/{titleId}", titleId)
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/history/watchlist/{titleId}/check returns 200 with status")
    void checkWatchlist_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();

        WatchlistStatusResponse response = new WatchlistStatusResponse(titleId, true);
        when(historyService.isInWatchlist(userId, titleId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/history/watchlist/{titleId}/check", titleId)
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleId").value(titleId.toString()))
                .andExpect(jsonPath("$.inWatchlist").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/history/progress/{contentId} returns the caller's saved position")
    void getProgress_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ViewingProgressResponse response = ViewingProgressResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .contentId(contentId)
                .positionSeconds(125)
                .durationSeconds(7200)
                .progressPercent(1.7)
                .completed(false)
                .lastWatchedAt(Instant.now())
                .build();

        when(historyService.getProgress(userId, contentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/history/progress/" + contentId)
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionSeconds").value(125))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/history/progress/{contentId} returns 404 when there is no progress")
    void getProgress_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(historyService.getProgress(userId, contentId)).thenThrow(
                new com.vyroflix.common.error.ApiException(com.vyroflix.common.error.ErrorCode.NOT_FOUND, "none"));

        mockMvc.perform(get("/api/v1/history/progress/" + contentId)
                        .with(authentication(authPrincipal(userId))))
                .andExpect(status().isNotFound());
    }
}
