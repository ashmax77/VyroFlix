package com.vyroflix.content.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoEncodedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.content.service.AdminCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoEncodedConsumerTest {

    @Mock
    private AdminCatalogService adminCatalogService;

    private ObjectMapper objectMapper;
    private VideoEncodedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new VideoEncodedConsumer(adminCatalogService, objectMapper);
    }

    @Test
    @DisplayName("consume updates media asset to READY on video.encoded.v1")
    void consume_ValidEventEnvelope() throws Exception {
        UUID contentId = UUID.randomUUID();
        String playlistKey = "titles/" + contentId + "/hls/master.m3u8";

        VideoEncodedEventPayload payload = VideoEncodedEventPayload.builder()
                .encodingJobId(UUID.randomUUID())
                .contentId(contentId)
                .masterPlaylistKey(playlistKey)
                .durationSeconds(5400)
                .encodedAt(Instant.now())
                .renditions(List.of())
                .build();

        EventEnvelope<VideoEncodedEventPayload> envelope = EventEnvelope.of(
                Constants.TOPIC_VIDEO_ENCODED,
                Constants.SERVICE_ENCODING,
                UUID.randomUUID(),
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        consumer.consume(json);

        verify(adminCatalogService).updateMediaAssetReady(contentId, playlistKey, 5400);
    }

    @Test
    @DisplayName("consume ignores blank message")
    void consume_BlankMessage_Ignored() {
        consumer.consume("");
        consumer.consume(null);

        verifyNoInteractions(adminCatalogService);
    }
}
