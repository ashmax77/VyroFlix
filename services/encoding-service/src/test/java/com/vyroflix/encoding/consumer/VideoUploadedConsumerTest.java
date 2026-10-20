package com.vyroflix.encoding.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoUploadedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.encoding.entity.EncodingJob;
import com.vyroflix.encoding.service.EncodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadedConsumerTest {

    @Mock
    private EncodingService encodingService;

    private ObjectMapper objectMapper;
    private VideoUploadedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new VideoUploadedConsumer(encodingService, objectMapper);
    }

    @Test
    @DisplayName("consume processes valid EventEnvelope and executes encoding")
    void consume_ValidEventEnvelope() throws Exception {
        UUID uploadIntentId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        VideoUploadedEventPayload payload = VideoUploadedEventPayload.builder()
                .uploadIntentId(uploadIntentId)
                .contentId(contentId)
                .s3Bucket("vyroflix-raw")
                .s3Key("raw/movie/" + contentId + "/film.mp4")
                .uploadedAt(Instant.now())
                .build();

        EventEnvelope<VideoUploadedEventPayload> envelope = EventEnvelope.of(
                Constants.TOPIC_VIDEO_UPLOADED,
                Constants.SERVICE_VIDEO,
                UUID.randomUUID(),
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        EncodingJob mockJob = EncodingJob.builder().uploadIntentId(uploadIntentId).contentId(contentId).build();
        mockJob.setId(jobId);
        when(encodingService.createOrGetJob(any(VideoUploadedEventPayload.class))).thenReturn(mockJob);

        consumer.consume(json);

        verify(encodingService).createOrGetJob(any(VideoUploadedEventPayload.class));
        verify(encodingService).executeEncoding(jobId);
    }

    @Test
    @DisplayName("consume ignores null or blank message")
    void consume_BlankMessage_Ignored() {
        consumer.consume("");
        consumer.consume(null);

        verifyNoInteractions(encodingService);
    }
}
