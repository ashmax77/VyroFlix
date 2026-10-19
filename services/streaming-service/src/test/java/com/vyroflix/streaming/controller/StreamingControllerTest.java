package com.vyroflix.streaming.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.dto.CreateSessionRequest;
import com.vyroflix.streaming.dto.PlaybackSessionResponse;
import com.vyroflix.streaming.service.StreamingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StreamingController.class)
@AutoConfigureMockMvc(addFilters = false)
class StreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StreamingService streamingService;

    @Test
    @DisplayName("POST /api/v1/streaming/sessions returns 201 with signed manifest URL")
    void createSession_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        CreateSessionRequest request = CreateSessionRequest.builder()
                .contentId(contentId)
                .targetType("MOVIE")
                .deviceId("web-client-1")
                .build();

        PlaybackSessionResponse response = PlaybackSessionResponse.builder()
                .sessionId(sessionId)
                .manifestUrl("https://cdn.vyroflix.com/vyroflix-content/hls/titles/" + contentId + "/master.m3u8?token=xyz")
                .expiresAt(Instant.now().plusSeconds(900))
                .resumePositionSeconds(0)
                .build();

        when(streamingService.createSession(eq(userId), any(CreateSessionRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/streaming/sessions")
                        .header(Constants.HEADER_USER_ID, userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.manifestUrl").value(response.getManifestUrl()));
    }

    @Test
    @DisplayName("POST /api/v1/streaming/sessions returns 401 when unauthenticated")
    void createSession_Unauthenticated() throws Exception {
        UUID contentId = UUID.randomUUID();
        CreateSessionRequest request = CreateSessionRequest.builder()
                .contentId(contentId)
                .build();

        mockMvc.perform(post("/api/v1/streaming/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
