package com.vyroflix.encoding.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoUploadedEventPayload;
import com.vyroflix.common.messaging.EventConsumer;
import com.vyroflix.common.util.Constants;
import com.vyroflix.encoding.entity.EncodingJob;
import com.vyroflix.encoding.service.EncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes video.uploaded.v1 events and triggers the FFmpeg transcoding pipeline.
 *
 * <p>Deduplicates incoming events idempotently via uploadIntentId.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadedConsumer implements EventConsumer<VideoUploadedEventPayload> {

    private final EncodingService encodingService;
    private final ObjectMapper objectMapper;

    @Override
    public String getEventType() {
        return Constants.TOPIC_VIDEO_UPLOADED;
    }

    @Override
    public void onEvent(EventEnvelope<VideoUploadedEventPayload> event) {
        log.info("Handling [{}] event: eventId=[{}] correlationId=[{}]",
                event.getEventType(), event.getEventId(), event.getCorrelationId());
        processPayload(event.getPayload());
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.video-uploaded:" + Constants.TOPIC_VIDEO_UPLOADED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-encoding-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        try {
            EventEnvelope<VideoUploadedEventPayload> envelope = objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<VideoUploadedEventPayload>>() {}
            );
            if (envelope != null && envelope.getPayload() != null) {
                onEvent(envelope);
                return;
            }
        } catch (Exception e) {
            log.debug("Direct envelope parse failed, falling back to payload: {}", e.getMessage());
        }

        try {
            VideoUploadedEventPayload payload = objectMapper.readValue(rawMessage, VideoUploadedEventPayload.class);
            processPayload(payload);
        } catch (Exception e) {
            log.error("Failed to parse Kafka message on topic [{}]: {}", getEventType(), e.getMessage(), e);
        }
    }

    public void processPayload(VideoUploadedEventPayload payload) {
        if (payload == null || payload.getUploadIntentId() == null) {
            log.warn("Ignoring empty or invalid VideoUploadedEventPayload");
            return;
        }

        log.info("Triggering transcode job for content [{}] uploadIntent [{}]",
                payload.getContentId(), payload.getUploadIntentId());

        EncodingJob job = encodingService.createOrGetJob(payload);
        encodingService.executeEncoding(job.getId());
    }
}
