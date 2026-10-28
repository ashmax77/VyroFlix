package com.vyroflix.streaming.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoEncodedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stores the master playlist key for an encoded asset so playback can be authorised.
 */
@Slf4j
@Component("streamingVideoEncodedConsumer")
@RequiredArgsConstructor
public class VideoEncodedConsumer {

    private final StreamingService streamingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.video-encoded:" + Constants.TOPIC_VIDEO_ENCODED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-streaming-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }
        EventEnvelope<VideoEncodedEventPayload> envelope = read(rawMessage);
        processPayload(envelope.getPayload());
    }

    public void processPayload(VideoEncodedEventPayload payload) {
        if (payload == null || payload.getContentId() == null || payload.getMasterPlaylistKey() == null) {
            log.warn("Ignoring {} without contentId/masterPlaylistKey", Constants.TOPIC_VIDEO_ENCODED);
            return;
        }
        log.info("Registering master playlist key for content [{}]", payload.getContentId());
        streamingService.registerMasterPlaylistKey(payload.getContentId(), payload.getMasterPlaylistKey());
    }

    /** Rethrows on failure so the container retries and then dead-letters the record. */
    private EventEnvelope<VideoEncodedEventPayload> read(String rawMessage) {
        try {
            EventEnvelope<VideoEncodedEventPayload> envelope = objectMapper.readValue(
                    rawMessage, new TypeReference<EventEnvelope<VideoEncodedEventPayload>>() {});
            if (envelope == null) {
                throw new IllegalArgumentException("Empty event envelope");
            }
            return envelope;
        } catch (Exception e) {
            log.error("Failed to parse {} event", Constants.TOPIC_VIDEO_ENCODED);
            throw new IllegalStateException("Unparseable video.encoded event", e);
        }
    }
}
