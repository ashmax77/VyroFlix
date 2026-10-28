package com.vyroflix.streaming.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoEncodedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.service.StreamingService;
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
    private StreamingService streamingService;

    private ObjectMapper objectMapper;
    private VideoEncodedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new VideoEncodedConsumer(streamingService, objectMapper);
    }

    @Test
    @DisplayName("consume caches master playlist key upon receiving video.encoded.v1")
    void consume_ValidEventEnvelope() throws Exception {
        UUID contentId = UUID.randomUUID();
        String playlistKey = "titles/" + contentId + "/hls/master.m3u8";

        VideoEncodedEventPayload payload = VideoEncodedEventPayload.builder()
                .encodingJobId(UUID.randomUUID())
                .contentId(contentId)
                .masterPlaylistKey(playlistKey)
                .durationSeconds(7200)
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

        verify(streamingService).registerMasterPlaylistKey(contentId, playlistKey);
    }

    @Test
    @DisplayName("consume ignores blank message")
    void consume_BlankMessage_Ignored() {
        consumer.consume("");
        consumer.consume(null);

        verifyNoInteractions(streamingService);
    }
}
