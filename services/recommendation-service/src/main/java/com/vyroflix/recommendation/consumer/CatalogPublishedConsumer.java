package com.vyroflix.recommendation.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.messaging.EventConsumer;
import com.vyroflix.common.util.Constants;
import com.vyroflix.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component("recommendationCatalogPublishedConsumer")
@RequiredArgsConstructor
public class CatalogPublishedConsumer implements EventConsumer<TitlePublishedEventPayload> {

    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    @Override
    public String getEventType() {
        return Constants.TOPIC_CATALOG_TITLE_PUBLISHED;
    }

    @Override
    public void onEvent(EventEnvelope<TitlePublishedEventPayload> event) {
        log.info("Handling [{}] event in recommendation service: titleId=[{}]",
                event.getEventType(), event.getPayload() != null ? event.getPayload().getTitleId() : null);
        processPayload(event.getPayload());
    }

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.catalog-title-published:" + Constants.TOPIC_CATALOG_TITLE_PUBLISHED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-recommendation-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;

        try {
            EventEnvelope<TitlePublishedEventPayload> envelope = objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<TitlePublishedEventPayload>>() {}
            );
            if (envelope != null && envelope.getPayload() != null) {
                onEvent(envelope);
                return;
            }
        } catch (Exception e) {
            log.debug("Direct envelope parse failed, trying direct payload: {}", e.getMessage());
        }

        try {
            TitlePublishedEventPayload payload = objectMapper.readValue(rawMessage, TitlePublishedEventPayload.class);
            processPayload(payload);
        } catch (Exception e) {
            log.error("Failed to parse catalog.title.published payload: {}", rawMessage, e);
        }
    }

    private void processPayload(TitlePublishedEventPayload payload) {
        if (payload != null && payload.getTitleId() != null) {
            recommendationService.indexTitleMetadata(payload);
        }
    }
}
