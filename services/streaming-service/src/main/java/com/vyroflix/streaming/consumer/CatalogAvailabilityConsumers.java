package com.vyroflix.streaming.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EpisodePublishedEventPayload;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.streaming.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps streaming's publication read model in step with Content-Service.
 *
 * <p>Failures are rethrown (not swallowed) so the container retries and then routes the record to the
 * {@code .DLT} topic. Writes are upserts, so redelivery is harmless.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogAvailabilityConsumers {

    private static final String GROUP_ID = "${spring.kafka.consumer.group-id:vyroflix-streaming-service}";

    private final StreamingService streamingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-title-published:" + Constants.TOPIC_CATALOG_TITLE_PUBLISHED + "}",
            groupId = GROUP_ID)
    public void onTitlePublished(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        EventEnvelope<TitlePublishedEventPayload> envelope = read(rawMessage, new TypeReference<>() {});
        TitlePublishedEventPayload payload = envelope.getPayload();
        if (payload == null || payload.getTitleId() == null) {
            log.warn("Ignoring {} without titleId", Constants.TOPIC_CATALOG_TITLE_PUBLISHED);
            return;
        }
        streamingService.recordTitlePublication(payload.getTitleId(), true);
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-title-unpublished:" + Constants.TOPIC_CATALOG_TITLE_UNPUBLISHED + "}",
            groupId = GROUP_ID)
    public void onTitleUnpublished(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        EventEnvelope<TitleUnpublishedEventPayload> envelope = read(rawMessage, new TypeReference<>() {});
        TitleUnpublishedEventPayload payload = envelope.getPayload();
        if (payload == null || payload.getTitleId() == null) {
            log.warn("Ignoring {} without titleId", Constants.TOPIC_CATALOG_TITLE_UNPUBLISHED);
            return;
        }
        streamingService.recordTitlePublication(payload.getTitleId(), false);
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-episode-published:" + Constants.TOPIC_CATALOG_EPISODE_PUBLISHED + "}",
            groupId = GROUP_ID)
    public void onEpisodePublished(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        EventEnvelope<EpisodePublishedEventPayload> envelope = read(rawMessage, new TypeReference<>() {});
        EpisodePublishedEventPayload payload = envelope.getPayload();
        if (payload == null || payload.getEpisodeId() == null || payload.getTitleId() == null) {
            log.warn("Ignoring {} without episodeId/titleId", Constants.TOPIC_CATALOG_EPISODE_PUBLISHED);
            return;
        }
        streamingService.recordEpisodePublication(payload.getEpisodeId(), payload.getTitleId());
    }

    private <T> EventEnvelope<T> read(String rawMessage, TypeReference<EventEnvelope<T>> type) {
        try {
            EventEnvelope<T> envelope = objectMapper.readValue(rawMessage, type);
            if (envelope == null) {
                throw new IllegalArgumentException("Empty event envelope");
            }
            return envelope;
        } catch (Exception e) {
            log.error("Failed to parse event on catalog topic; routing to dead-letter after retries");
            throw new IllegalStateException("Unparseable catalog event", e);
        }
    }
}
