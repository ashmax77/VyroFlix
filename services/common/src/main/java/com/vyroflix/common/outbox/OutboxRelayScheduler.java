package com.vyroflix.common.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Scheduled relay polling unpublished outbox records and publishing them via {@link EventPublisher}.
 *
 * <p>Polls at a configurable interval ({@code vyroflix.outbox.relay-interval-ms}).
 * Updates status to {@code PUBLISHED} upon success, or handles retries/failure.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    /**
     * Polls pending outbox events and publishes them to the message broker.
     */
    @Scheduled(fixedDelayString = "${vyroflix.outbox.relay-interval-ms:2000}")
    public void relayPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxService.findPendingEvents(properties.getBatchSize());
            if (pendingEvents == null || pendingEvents.isEmpty()) {
                return;
            }

            log.debug("Found {} pending outbox event(s) to relay", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                relayEvent(event);
            }
        } catch (Exception e) {
            log.error("Error during outbox relay execution: {}", e.getMessage(), e);
        }
    }

    private void relayEvent(OutboxEvent event) {
        try {
            Object payload;
            try {
                payload = objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                payload = event.getPayload();
            }

            EventEnvelope<Object> envelope = EventEnvelope.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .occurredAt(event.getCreatedAt())
                    .producer(event.getProducer())
                    .correlationId(event.getCorrelationId())
                    .payload(payload)
                    .build();

            String topic = event.getEventType();
            String key = event.getEventId().toString();

            log.debug("Relaying outbox event [{}] to topic [{}]", event.getEventId(), topic);

            eventPublisher.publish(topic, key, envelope);
            outboxService.markPublished(event.getId());

            log.debug("Successfully relayed outbox event [{}]", event.getEventId());
        } catch (Exception e) {
            log.warn("Failed to relay outbox event [{}]: {}", event.getEventId(), e.getMessage());
            outboxService.handleFailure(event.getId(), properties.getMaxRetries(), e);
        }
    }

    /**
     * Scheduled cleanup of published records older than {@code vyroflix.outbox.retention-days}.
     */
    @Scheduled(cron = "${vyroflix.outbox.cleanup-cron:0 0 2 * * ?}")
    public void cleanupOldEvents() {
        try {
            Instant cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
            outboxService.cleanupPublished(cutoff);
        } catch (Exception e) {
            log.error("Error during outbox cleanup: {}", e.getMessage(), e);
        }
    }
}
