package com.vyroflix.common.messaging;

import com.vyroflix.common.event.EventEnvelope;

/**
 * Marker and contract interface for domain event consumers.
 *
 * <p>Consumers must be idempotent by deduplicating using {@link EventEnvelope#getEventId()}
 * or an equivalent business entity key.</p>
 *
 * @param <T> event payload type
 */
public interface EventConsumer<T> {

    /**
     * Handles an incoming event envelope.
     *
     * @param event the event envelope containing metadata and domain payload
     */
    void onEvent(EventEnvelope<T> event);

    /**
     * Returns the versioned event type this consumer handles (e.g. {@code "video.uploaded.v1"}).
     *
     * @return versioned event type string
     */
    String getEventType();
}
