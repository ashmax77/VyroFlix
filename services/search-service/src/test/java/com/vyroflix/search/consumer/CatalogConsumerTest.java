package com.vyroflix.search.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogConsumerTest {

    @Mock
    private SearchService searchService;

    private ObjectMapper objectMapper;
    private CatalogPublishedConsumer publishedConsumer;
    private CatalogUnpublishedConsumer unpublishedConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        publishedConsumer = new CatalogPublishedConsumer(searchService, objectMapper);
        unpublishedConsumer = new CatalogUnpublishedConsumer(searchService, objectMapper);
    }

    @Test
    @DisplayName("CatalogPublishedConsumer consumes event and forwards payload to searchService.indexTitle")
    void consumesPublishedEventEnvelope() throws Exception {
        UUID titleId = UUID.randomUUID();
        TitlePublishedEventPayload payload = TitlePublishedEventPayload.builder()
                .titleId(titleId)
                .title("Dune: Part Two")
                .type("MOVIE")
                .synopsis("Paul Atreides unites with Chani and the Fremen.")
                .genreSlugs(Set.of("sci-fi", "adventure"))
                .releaseYear(2024)
                .maturityRating("PG-13")
                .publishedAt(Instant.now())
                .build();

        EventEnvelope<TitlePublishedEventPayload> envelope = EventEnvelope.of(
                "catalog.title.published.v1",
                "content-service",
                UUID.randomUUID(),
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        publishedConsumer.consume(json);

        ArgumentCaptor<TitlePublishedEventPayload> captor = ArgumentCaptor.forClass(TitlePublishedEventPayload.class);
        verify(searchService).indexTitle(captor.capture());

        TitlePublishedEventPayload captured = captor.getValue();
        assertThat(captured.getTitleId()).isEqualTo(titleId);
        assertThat(captured.getTitle()).isEqualTo("Dune: Part Two");
    }

    @Test
    @DisplayName("CatalogUnpublishedConsumer consumes event and forwards payload to searchService.unindexTitle")
    void consumesUnpublishedEventEnvelope() throws Exception {
        UUID titleId = UUID.randomUUID();
        TitleUnpublishedEventPayload payload = TitleUnpublishedEventPayload.builder()
                .titleId(titleId)
                .unpublishedAt(Instant.now())
                .reason("License expired")
                .build();

        EventEnvelope<TitleUnpublishedEventPayload> envelope = EventEnvelope.of(
                "catalog.title.unpublished.v1",
                "content-service",
                UUID.randomUUID(),
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        unpublishedConsumer.consume(json);

        ArgumentCaptor<TitleUnpublishedEventPayload> captor = ArgumentCaptor.forClass(TitleUnpublishedEventPayload.class);
        verify(searchService).unindexTitle(captor.capture());

        TitleUnpublishedEventPayload captured = captor.getValue();
        assertThat(captured.getTitleId()).isEqualTo(titleId);
    }
}
