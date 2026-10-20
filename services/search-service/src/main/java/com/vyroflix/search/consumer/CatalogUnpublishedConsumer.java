package com.vyroflix.search.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.common.messaging.EventConsumer;
import com.vyroflix.common.util.Constants;
import com.vyroflix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes catalog.title.unpublished.v1 events and marks titles UNPUBLISHED in the search index.
 */
@Slf4j
@Component("searchCatalogUnpublishedConsumer")
@RequiredArgsConstructor
public class CatalogUnpublishedConsumer implements EventConsumer<TitleUnpublishedEventPayload> {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    @Override
    public String getEventType() {
        return Constants.TOPIC_CATALOG_TITLE_UNPUBLISHED;
    }

    @Override
    public void onEvent(EventEnvelope<TitleUnpublishedEventPayload> event) {
        log.info("Handling [{}] event: eventId=[{}] titleId=[{}]",
                event.getEventType(), event.getEventId(),
                event.getPayload() != null ? event.getPayload().getTitleId() : null);
        processPayload(event.getPayload());
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-title-unpublished:" + Constants.TOPIC_CATALOG_TITLE_UNPUBLISHED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-search-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        try {
            EventEnvelope<TitleUnpublishedEventPayload> envelope = objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<TitleUnpublishedEventPayload>>() {}
            );
            if (envelope != null && envelope.getPayload() != null) {
                onEvent(envelope);
                return;
            }
        } catch (Exception e) {
            log.debug("Direct envelope parse failed, trying direct payload: {}", e.getMessage());
        }

        try {
            TitleUnpublishedEventPayload payload = objectMapper.readValue(rawMessage, TitleUnpublishedEventPayload.class);
            processPayload(payload);
        } catch (Exception e) {
            log.error("Failed to deserialize catalog.title.unpublished payload: {}", rawMessage, e);
        }
    }

    private void processPayload(TitleUnpublishedEventPayload payload) {
        if (payload == null || payload.getTitleId() == null) {
            log.warn("Received empty or invalid TitleUnpublishedEventPayload");
            return;
        }
        searchService.unindexTitle(payload);
    }
}
