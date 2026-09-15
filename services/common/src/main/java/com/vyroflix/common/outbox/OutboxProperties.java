package com.vyroflix.common.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the transactional outbox relay and cleanup.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vyroflix.outbox")
public class OutboxProperties {

    /**
     * Whether the outbox relay scheduler is enabled. Defaults to {@code true}.
     */
    private boolean enabled = true;

    /**
     * Fixed delay in milliseconds between outbox relay execution cycles. Defaults to 2000 ms.
     */
    private long relayIntervalMs = 2000;

    /**
     * Maximum number of pending outbox events to fetch per polling batch. Defaults to 50.
     */
    private int batchSize = 50;

    /**
     * Maximum retry attempts before an event is marked as {@link OutboxStatus#FAILED}. Defaults to 3.
     */
    private int maxRetries = 3;

    /**
     * Number of days to retain published outbox events before purge. Defaults to 7 days.
     */
    private int retentionDays = 7;
}
