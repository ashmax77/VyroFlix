package com.vyroflix.content.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoEncodedEventPayload;
import com.vyroflix.common.messaging.EventConsumer;
import com.vyroflix.common.util.Constants;
import com.vyroflix.content.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes video.encoded.v1 events and marks media asset references as READY in the catalog.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodedConsumer implements EventConsumer<VideoEncodedEventPayload> {

    private final AdminCatalogService adminCatalogService;
    private final ObjectMapper objectMapper;

    @Override
    public String getEventType() {
        return Constants.TOPIC_VIDEO_ENCODED;
    }

    @Override
    public void onEvent(EventEnvelope<VideoEncodedEventPayload> event) {
        log.info("Handling [{}] event: eventId=[{}] correlationId=[{}]",
                event.getEventType(), event.getEventId(), event.getCorrelationId());
        processPayload(event.getPayload());
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.video-encoded:" + Constants.TOPIC_VIDEO_ENCODED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-content-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        try {
            EventEnvelope<VideoEncodedEventPayload> envelope = objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<VideoEncodedEventPayload>>() {}
            );
            if (envelope != null && envelope.getPayload() != null) {
                onEvent(envelope);
                return;
            }
        } catch (Exception e) {
            log.debug("Direct envelope parse failed, trying payload: {}", e.getMessage());
        }

        try {
            VideoEncodedEventPayload payload = objectMapper.readValue(rawMessage, VideoEncodedEventPayload.class);
            processPayload(payload);
        } catch (Exception e) {
            log.error("Failed to parse Kafka message on topic [{}]: {}", getEventType(), e.getMessage(), e);
        }
    }

    public void processPayload(VideoEncodedEventPayload payload) {
        if (payload == null || payload.getContentId() == null) {
            log.warn("Ignoring invalid VideoEncodedEventPayload: {}", payload);
            return;
        }

        log.info("Updating media asset READY for contentId [{}]", payload.getContentId());
        adminCatalogService.updateMediaAssetReady(
                payload.getContentId(),
                payload.getMasterPlaylistKey(),
                payload.getDurationSeconds()
        );
    }
}
