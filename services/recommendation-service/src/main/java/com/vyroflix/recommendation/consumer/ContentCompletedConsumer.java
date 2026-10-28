package com.vyroflix.recommendation.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.util.Constants;
import com.vyroflix.recommendation.service.RecommendationEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("recommendationContentCompletedConsumer")
@RequiredArgsConstructor
public class ContentCompletedConsumer {

    private final RecommendationEventProcessor eventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.history-content-completed:" + Constants.TOPIC_HISTORY_CONTENT_COMPLETED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-recommendation-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;

        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            JsonNode payload = root.has("payload") ? root.get("payload") : root;
            String eventId = root.hasNonNull("eventId") ? root.get("eventId").asText() : null;

            if (payload.has("contentId")) {
                UUID contentId = UUID.fromString(payload.get("contentId").asText());
                eventProcessor.onContentCompleted(eventId, contentId);
            }
        } catch (Exception e) {
            // Rethrow: the container retries with backoff, then routes to the .DLT topic.
            log.error("Failed to process history.content.completed event", e);
            throw new IllegalStateException("Failed to process history.content.completed event", e);
        }
    }
}
