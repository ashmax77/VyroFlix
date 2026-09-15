package com.vyroflix.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.messaging.EventPublisher;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring auto-configuration for the Transactional Outbox pattern.
 *
 * <p>Auto-registers {@link OutboxRepository}, {@link OutboxService},
 * and conditionally {@link OutboxRelayScheduler} when an {@link EntityManager}
 * and {@link EventPublisher} are present.</p>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@EnableScheduling
@ConditionalOnClass(EntityManager.class)
@ConditionalOnBean(EntityManager.class)
@EnableConfigurationProperties(OutboxProperties.class)
@ConditionalOnProperty(prefix = "vyroflix.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackageClasses = OutboxEvent.class)
public class OutboxConfig {

    @Bean
    @ConditionalOnMissingBean(OutboxRepository.class)
    public OutboxRepository outboxRepository(EntityManager entityManager) {
        return new OutboxRepository(entityManager);
    }

    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    @ConditionalOnMissingBean(OutboxService.class)
    public OutboxService outboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        return new OutboxService(outboxRepository, objectMapper);
    }

    @Bean
    @ConditionalOnBean({OutboxService.class, EventPublisher.class})
    @ConditionalOnMissingBean(OutboxRelayScheduler.class)
    public OutboxRelayScheduler outboxRelayScheduler(
            OutboxService outboxService,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            OutboxProperties properties) {
        return new OutboxRelayScheduler(outboxService, eventPublisher, objectMapper, properties);
    }
}
