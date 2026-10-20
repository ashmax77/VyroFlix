package com.vyroflix.encoding.controller;

import com.vyroflix.common.error.GlobalExceptionHandler;
import com.vyroflix.encoding.dto.EncodingJobResponse;
import com.vyroflix.encoding.dto.HlsRenditionResponse;
import com.vyroflix.encoding.entity.EncodingStatus;
import com.vyroflix.encoding.service.EncodingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EncodingJobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EncodingJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EncodingService encodingService;

    @Test
    @DisplayName("GET /api/v1/encoding/jobs returns paged list of jobs")
    void getJobs_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        EncodingJobResponse response = EncodingJobResponse.builder()
                .id(jobId)
                .contentId(UUID.randomUUID())
                .status(EncodingStatus.READY)
                .build();

        when(encodingService.getJobs(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/encoding/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("READY"));
    }

    @Test
    @DisplayName("GET /api/v1/encoding/jobs/{id} returns job details and renditions")
    void getJob_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        EncodingJobResponse response = EncodingJobResponse.builder()
                .id(jobId)
                .contentId(contentId)
                .status(EncodingStatus.READY)
                .masterPlaylistKey("titles/" + contentId + "/hls/master.m3u8")
                .renditions(List.of(
                        HlsRenditionResponse.builder().resolution("1080p").bitrate(4500000).build()
                ))
                .build();

        when(encodingService.getJob(jobId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/encoding/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.renditions[0].resolution").value("1080p"));
    }

    @Test
    @DisplayName("POST /api/v1/encoding/jobs/{id}/retry triggers retry and returns 200 OK")
    void retryJob_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        EncodingJobResponse response = EncodingJobResponse.builder()
                .id(jobId)
                .status(EncodingStatus.READY)
                .retryCount(1)
                .build();

        when(encodingService.retryJob(jobId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/encoding/jobs/{id}/retry", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.retryCount").value(1));
    }
}
