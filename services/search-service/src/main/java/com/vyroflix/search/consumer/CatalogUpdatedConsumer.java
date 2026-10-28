package com.vyroflix.search.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.util.Constants;
import com.vyroflix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the search index current when a published title's metadata or credits change.
 * Upserts, so redelivery is harmless; failures are rethrown for retry and dead-lettering.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogUpdatedConsumer {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-title-updated:" + Constants.TOPIC_CATALOG_TITLE_UPDATED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-search-service}")
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;

        EventEnvelope<TitlePublishedEventPayload> envelope;
        try {
            envelope = objectMapper.readValue(rawMessage, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse {} event", Constants.TOPIC_CATALOG_TITLE_UPDATED);
            throw new IllegalStateException("Unparseable catalog.title.updated event", e);
        }
        TitlePublishedEventPayload payload = envelope == null ? null : envelope.getPayload();
        if (payload == null || payload.getTitleId() == null) {
            log.warn("Ignoring {} without titleId", Constants.TOPIC_CATALOG_TITLE_UPDATED);
            return;
        }
        searchService.indexTitle(payload);
    }
}
