package com.vyroflix.common.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.messaging.EventPublisher;
import com.vyroflix.common.messaging.MessagingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spring configuration for Kafka-based event publishing.
 *
 * <p>Activated when {@code vyroflix.messaging.type=kafka} (the default).</p>
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(prefix = "vyroflix.messaging", name = "type", havingValue = "kafka", matchIfMissing = true)
public class KafkaConfig {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher kafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        return new KafkaEventPublisher(kafkaTemplate, objectMapper);
    }
}
