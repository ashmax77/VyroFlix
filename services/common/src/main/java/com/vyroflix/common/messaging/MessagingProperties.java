package com.vyroflix.common.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the VyroFlix messaging abstraction layer.
 *
 * <p>Switches between standard Apache Kafka (local) and Upstash Kafka REST (production).</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vyroflix.messaging")
public class MessagingProperties {

    /**
     * Provider type: {@code KAFKA} (standard Apache Kafka via KafkaTemplate) or
     * {@code UPSTASH} (Upstash Kafka via REST API).
     */
    private ProviderType type = ProviderType.KAFKA;

    /**
     * Upstash REST configuration.
     */
    private Upstash upstash = new Upstash();

    public enum ProviderType {
        KAFKA,
        UPSTASH
    }

    @Getter
    @Setter
    public static class Upstash {
        /**
         * Base URL for the Upstash Kafka REST API (e.g. {@code https://distinct-fawn-12345-us1-rest-kafka.upstash.io}).
         */
        private String restUrl;

        /**
         * Secret token for authenticating with the Upstash Kafka REST API.
         */
        private String restToken;
    }
}
