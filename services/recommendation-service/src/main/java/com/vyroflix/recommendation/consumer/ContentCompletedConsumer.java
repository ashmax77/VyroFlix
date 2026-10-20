package com.vyroflix.recommendation.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.util.Constants;
import com.vyroflix.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("recommendationContentCompletedConsumer")
@RequiredArgsConstructor
public class ContentCompletedConsumer {

    private final RecommendationService recommendationService;
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

            if (payload.has("contentId")) {
                UUID contentId = UUID.fromString(payload.get("contentId").asText());
                recommendationService.recordViewingProgress(contentId, true);
            }
        } catch (Exception e) {
            log.error("Failed to parse history.content.completed event: {}", rawMessage, e);
        }
    }
}
