package com.vyroflix.common.outbox;

/**
 * Lifecycle status of a transactional outbox event.
 */
public enum OutboxStatus {
    /**
     * Event written in business transaction; awaiting relay to Kafka.
     */
    PENDING,

    /**
     * Successfully published to the messaging broker.
     */
    PUBLISHED,

    /**
     * Failed publication after exhausting configured retry attempts.
     */
    FAILED
}
