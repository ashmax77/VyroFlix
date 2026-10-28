package com.vyroflix.streaming.repository;

import com.vyroflix.streaming.entity.MasterPlaylistKey;
import com.vyroflix.streaming.entity.TitleAvailability;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real V1 and V2 migrations against PostgreSQL, then round-trips the availability and playlist-key
 * read models. Requires Docker (runs in CI).
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StreamingReadModelsIntegrationTest {

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

    @Autowired
    private TitleAvailabilityRepository availabilityRepository;

    @Autowired
    private MasterPlaylistKeyRepository masterPlaylistKeyRepository;

    @Test
    void episodeAvailabilityPointsAtItsSeries() {
        UUID series = UUID.randomUUID();
        UUID episode = UUID.randomUUID();
        availabilityRepository.save(TitleAvailability.builder()
                .contentId(series).published(true).updatedAt(Instant.now()).build());
        availabilityRepository.save(TitleAvailability.builder()
                .contentId(episode).parentId(series).published(true).updatedAt(Instant.now()).build());

        TitleAvailability row = availabilityRepository.findById(episode).orElseThrow();
        assertThat(row.getParentId()).isEqualTo(series);
        assertThat(availabilityRepository.findById(series).orElseThrow().isPublished()).isTrue();
    }

    @Test
    void masterPlaylistKeyIsUpsertedByContentId() {
        UUID content = UUID.randomUUID();
        masterPlaylistKeyRepository.save(MasterPlaylistKey.builder()
                .contentId(content).playlistKey("titles/" + content + "/hls/master.m3u8").updatedAt(Instant.now()).build());
        masterPlaylistKeyRepository.save(MasterPlaylistKey.builder()
                .contentId(content).playlistKey("titles/" + content + "/hls/v2/master.m3u8").updatedAt(Instant.now()).build());

        assertThat(masterPlaylistKeyRepository.findById(content).orElseThrow().getPlaylistKey())
                .endsWith("/hls/v2/master.m3u8");
        assertThat(masterPlaylistKeyRepository.count()).isEqualTo(1);
    }
}
