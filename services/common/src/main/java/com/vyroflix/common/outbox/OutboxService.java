package com.vyroflix.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service managing transactional outbox writes and state transitions.
 *
 * <p>Domain services call {@link #save(EventEnvelope)} inside their database
 * transaction to guarantee atomic persistence of domain changes and outgoing events.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves an event to the outbox table within the caller's active database transaction.
     *
     * @param envelope the event envelope to persist
     * @return the saved outbox event record
     */
    @Transactional
    public OutboxEvent save(EventEnvelope<?> envelope) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope.getPayload());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(envelope.getEventId())
                    .eventType(envelope.getEventType())
                    .producer(envelope.getProducer())
                    .correlationId(envelope.getCorrelationId())
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(envelope.getOccurredAt() != null ? envelope.getOccurredAt() : Instant.now())
                    .build();

            log.debug("Saving outbox event: eventId={}, eventType={}, producer={}",
                    outboxEvent.getEventId(), outboxEvent.getEventType(), outboxEvent.getProducer());

            return outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for eventId={}: {}",
                    envelope.getEventId(), e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to serialize outbox event payload", e);
        }
    }

    /**
     * Convenience factory method to construct an {@link EventEnvelope} and save it to the outbox.
     *
     * @param eventType     versioned event type (e.g. {@code "video.uploaded.v1"})
     * @param producer      producing service name
     * @param correlationId correlation ID for tracing
     * @param payload       domain payload object
     * @param <T>           payload type
     * @return the saved outbox event record
     */
    @Transactional
    public <T> OutboxEvent save(String eventType, String producer, UUID correlationId, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, producer, correlationId, payload);
        return save(envelope);
    }

    /**
     * Retrieves pending outbox events ordered by creation timestamp ascending.
     *
     * @param limit maximum events to retrieve
     * @return list of pending outbox events
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int limit) {
        return outboxRepository.findPendingEvents(limit);
    }

    /**
     * Marks an outbox event as successfully published.
     *
     * @param id outbox record UUID
     */
    @Transactional
    public void markPublished(UUID id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.markPublished();
            outboxRepository.save(event);
            log.debug("Marked outbox event [{}] as PUBLISHED", id);
        });
    }

    /**
     * Handles publication failure for an outbox event by incrementing retries or marking FAILED.
     *
     * @param id         outbox record UUID
     * @param maxRetries retry threshold before marking FAILED
     * @param ex         cause of failure
     */
    @Transactional
    public void handleFailure(UUID id, int maxRetries, Exception ex) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.incrementRetry();
            if (event.getRetryCount() >= maxRetries) {
                event.markFailed();
                log.error("Outbox event [{}] exceeded max retries ({}/{}). Marked as FAILED: {}",
                        id, event.getRetryCount(), maxRetries, ex.getMessage());
            } else {
                log.warn("Outbox event [{}] failed attempt {}/{}: {}",
                        id, event.getRetryCount(), maxRetries, ex.getMessage());
            }
            outboxRepository.save(event);
        });
    }

    /**
     * Cleans up published outbox events older than the given cutoff timestamp.
     *
     * @param cutoff cutoff timestamp
     * @return count of deleted records
     */
    @Transactional
    public int cleanupPublished(Instant cutoff) {
        int deleted = outboxRepository.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} published outbox records older than {}", deleted, cutoff);
        }
        return deleted;
    }
}
