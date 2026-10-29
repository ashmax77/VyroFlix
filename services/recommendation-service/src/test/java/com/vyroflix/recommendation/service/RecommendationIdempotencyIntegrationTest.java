package com.vyroflix.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.recommendation.entity.TrendingScore;
import com.vyroflix.recommendation.repository.ProcessedEventRepository;
import com.vyroflix.recommendation.repository.TrendingScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Duplicate delivery against a real PostgreSQL: the idempotency claim is an INSERT ... ON CONFLICT, which an
 * in-memory mock cannot prove. Requires Docker (runs in CI; skipped by nothing else).
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RecommendationService.class, RecommendationEventProcessor.class})
class RecommendationIdempotencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vyroflix")
            .withInitScript("init-schema.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class Beans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private RecommendationEventProcessor processor;

    @Autowired
    private TrendingScoreRepository trendingScoreRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void redeliveredPlaybackStartedIsCountedOnce() {
        UUID titleId = UUID.randomUUID();

        processor.onPlaybackStarted("evt-redelivered-1", titleId);
        processor.onPlaybackStarted("evt-redelivered-1", titleId);

        TrendingScore score = trendingScoreRepository.findByTitleId(titleId).orElseThrow();
        assertThat(score.getViewCount()).isEqualTo(1);
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void distinctEventsAreEachCounted() {
        UUID titleId = UUID.randomUUID();

        processor.onContentCompleted("evt-a", titleId);
        processor.onContentCompleted("evt-b", titleId);

        TrendingScore score = trendingScoreRepository.findByTitleId(titleId).orElseThrow();
        assertThat(score.getCompletionCount()).isEqualTo(2);
    }
}
