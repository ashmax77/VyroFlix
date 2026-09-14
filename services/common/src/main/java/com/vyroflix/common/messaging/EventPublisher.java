package com.vyroflix.common.messaging;

import com.vyroflix.common.event.EventEnvelope;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for publishing domain events across VyroFlix.
 *
 * <p>Supports both local Apache Kafka (via {@code KafkaTemplate})
 * and production Upstash Kafka (via REST API / HTTP).
 * Implementations wrap events in the standard {@link EventEnvelope}.</p>
 */
public interface EventPublisher {

    /**
     * Publishes an event to the specified topic asynchronously using {@code event.getEventId().toString()} as the partition key.
     *
     * @param topic destination topic name
     * @param event the event envelope to publish
     * @return a future completing when the publish operation succeeds or fails
     */
    CompletableFuture<Void> publishAsync(String topic, EventEnvelope<?> event);

    /**
     * Publishes an event to the specified topic asynchronously with an explicit partition key.
     *
     * @param topic destination topic name
     * @param key   partition key for ordering guarantees (e.g. titleId, userId)
     * @param event the event envelope to publish
     * @return a future completing when the publish operation succeeds or fails
     */
    CompletableFuture<Void> publishAsync(String topic, String key, EventEnvelope<?> event);

    /**
     * Publishes an event synchronously (blocking until acknowledgment).
     *
     * @param topic destination topic name
     * @param event the event envelope to publish
     */
    default void publish(String topic, EventEnvelope<?> event) {
        publishAsync(topic, event).join();
    }

    /**
     * Publishes an event synchronously with an explicit partition key.
     *
     * @param topic destination topic name
     * @param key   partition key for ordering guarantees
     * @param event the event envelope to publish
     */
    default void publish(String topic, String key, EventEnvelope<?> event) {
        publishAsync(topic, key, event).join();
    }
}
