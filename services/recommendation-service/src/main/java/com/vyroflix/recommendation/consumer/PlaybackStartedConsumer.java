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
@Component("recommendationPlaybackStartedConsumer")
@RequiredArgsConstructor
public class PlaybackStartedConsumer {

    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${vyroflix.kafka.topics.playback-started:" + Constants.TOPIC_PLAYBACK_STARTED + "}",
            groupId = "${spring.kafka.consumer.group-id:vyroflix-recommendation-service}"
    )
    public void consume(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;

        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            if (payload.has("contentId")) {
                UUID contentId = UUID.fromString(payload.get("contentId").asText());
                recommendationService.recordPlaybackStarted(contentId);
            } else if (payload.has("titleId")) {
                UUID titleId = UUID.fromString(payload.get("titleId").asText());
                recommendationService.recordPlaybackStarted(titleId);
            }
        } catch (Exception e) {
            log.error("Failed to parse playback.started event: {}", rawMessage, e);
        }
    }
}
