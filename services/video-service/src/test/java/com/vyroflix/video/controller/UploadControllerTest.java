package com.vyroflix.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.error.GlobalExceptionHandler;
import com.vyroflix.video.dto.CreateUploadIntentRequest;
import com.vyroflix.video.dto.UploadIntentResponse;
import com.vyroflix.video.entity.TargetType;
import com.vyroflix.video.entity.UploadStatus;
import com.vyroflix.video.service.UploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UploadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UploadService uploadService;

    @Test
    @DisplayName("POST /api/v1/videos/upload-intents returns 201 CREATED with presigned upload details")
    void createUploadIntent_Success() throws Exception {
        UUID contentId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();

        CreateUploadIntentRequest request = CreateUploadIntentRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .filename("intro.mp4")
                .contentType("video/mp4")
                .fileSizeBytes(15000000L)
                .build();

        UploadIntentResponse response = UploadIntentResponse.builder()
                .id(intentId)
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .filename("intro.mp4")
                .contentType("video/mp4")
                .fileSizeBytes(15000000L)
                .s3Bucket("vyroflix-raw")
                .s3Key("raw/movie/" + contentId + "/intro.mp4")
                .status(UploadStatus.PENDING)
                .uploadUrl("https://storage.vyroflix.com/upload")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        when(uploadService.createUploadIntent(any(CreateUploadIntentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/videos/upload-intents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.uploadUrl").value("https://storage.vyroflix.com/upload"));
    }

    @Test
    @DisplayName("POST /api/v1/videos/upload-intents with missing required fields returns 422 Unprocessable Entity")
    void createUploadIntent_ValidationError() throws Exception {
        CreateUploadIntentRequest invalidRequest = CreateUploadIntentRequest.builder()
                .contentId(null)
                .targetType(null)
                .filename("")
                .build();

        mockMvc.perform(post("/api/v1/videos/upload-intents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/v1/videos/upload-intents/{id}/complete returns 200 OK with UPLOADED status")
    void completeUploadIntent_Success() throws Exception {
        UUID intentId = UUID.randomUUID();
        UploadIntentResponse response = UploadIntentResponse.builder()
                .id(intentId)
                .contentId(UUID.randomUUID())
                .targetType(TargetType.MOVIE)
                .filename("feature.mp4")
                .status(UploadStatus.UPLOADED)
                .build();

        when(uploadService.completeUploadIntent(intentId)).thenReturn(response);

        mockMvc.perform(put("/api/v1/videos/upload-intents/{id}/complete", intentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    @DisplayName("GET /api/v1/videos/upload-intents/{id} returns 200 OK")
    void getUploadIntent_Success() throws Exception {
        UUID intentId = UUID.randomUUID();
        UploadIntentResponse response = UploadIntentResponse.builder()
                .id(intentId)
                .contentId(UUID.randomUUID())
                .targetType(TargetType.EPISODE)
                .filename("s01e01.mp4")
                .status(UploadStatus.PENDING)
                .build();

        when(uploadService.getUploadIntent(intentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/videos/upload-intents/{id}", intentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.targetType").value("EPISODE"));
    }

    @Test
    @DisplayName("GET /api/v1/videos/upload-intents/content/{contentId} returns 200 OK with intent list")
    void getIntentsByContent_Success() throws Exception {
        UUID contentId = UUID.randomUUID();
        UploadIntentResponse r1 = UploadIntentResponse.builder().id(UUID.randomUUID()).contentId(contentId).build();
        UploadIntentResponse r2 = UploadIntentResponse.builder().id(UUID.randomUUID()).contentId(contentId).build();

        when(uploadService.getIntentsByContentId(contentId)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/videos/upload-intents/content/{contentId}", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
