package com.vyroflix.search.service;

import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.search.dto.SearchResultPage;
import com.vyroflix.search.entity.SearchDocument;
import com.vyroflix.search.repository.SearchDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchDocumentRepository searchDocumentRepository;

    @InjectMocks
    private SearchService searchService;

    private SearchDocument testDoc;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testDoc = SearchDocument.builder()
                .id(testId)
                .title("Interstellar")
                .type("MOVIE")
                .synopsis("A team of explorers travel through a wormhole in space.")
                .releaseYear(2014)
                .maturityRating("PG-13")
                .genres(List.of("sci-fi", "drama"))
                .posterUrl("https://r2.vyroflix.com/posters/interstellar.jpg")
                .publicationState("PUBLISHED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("search() with keyword triggers fullTextSearch repository method")
    void searchWithQueryTriggersFullTextSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SearchDocument> page = new PageImpl<>(List.of(testDoc), pageable, 1);

        when(searchDocumentRepository.fullTextSearch(eq("interstellar"), isNull(), eq(pageable)))
                .thenReturn(page);

        SearchResultPage result = searchService.search(" interstellar ", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Interstellar");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(searchDocumentRepository).fullTextSearch("interstellar", null, pageable);
    }

    @Test
    @DisplayName("search() with blank query returns default published titles ordered by release year")
    void searchWithBlankQueryReturnsDefaultTitles() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SearchDocument> page = new PageImpl<>(List.of(testDoc), pageable, 1);

        when(searchDocumentRepository.findByPublicationStateOrderByReleaseYearDesc("PUBLISHED", pageable))
                .thenReturn(page);

        SearchResultPage result = searchService.search("   ", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(searchDocumentRepository).findByPublicationStateOrderByReleaseYearDesc("PUBLISHED", pageable);
        verify(searchDocumentRepository, never()).fullTextSearch(any(), any(), any());
    }

    @Test
    @DisplayName("getSuggestions() queries repository with prefix and bounded limit")
    void getSuggestionsQueriesRepository() {
        when(searchDocumentRepository.findSuggestions("inte", 5))
                .thenReturn(List.of("Interstellar"));

        List<String> suggestions = searchService.getSuggestions("inte", 5);

        assertThat(suggestions).containsExactly("Interstellar");
        verify(searchDocumentRepository).findSuggestions("inte", 5);
    }

    @Test
    @DisplayName("indexTitle() saves new document when not found in repository")
    void indexTitleSavesNewDocument() {
        UUID newId = UUID.randomUUID();
        TitlePublishedEventPayload payload = TitlePublishedEventPayload.builder()
                .titleId(newId)
                .title("Inception")
                .type("MOVIE")
                .synopsis("A thief who steals corporate secrets through dream-sharing technology.")
                .genreSlugs(Set.of("action", "sci-fi"))
                .releaseYear(2010)
                .maturityRating("PG-13")
                .posterUrl("https://r2.vyroflix.com/posters/inception.jpg")
                .publishedAt(Instant.now())
                .build();

        when(searchDocumentRepository.findById(newId)).thenReturn(Optional.empty());

        searchService.indexTitle(payload);

        ArgumentCaptor<SearchDocument> captor = ArgumentCaptor.forClass(SearchDocument.class);
        verify(searchDocumentRepository).save(captor.capture());

        SearchDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(newId);
        assertThat(saved.getTitle()).isEqualTo("Inception");
        assertThat(saved.getType()).isEqualTo("MOVIE");
        assertThat(saved.getPublicationState()).isEqualTo("PUBLISHED");
        assertThat(saved.getGenres()).containsExactlyInAnyOrder("action", "sci-fi");
    }

    @Test
    @DisplayName("unindexTitle() marks document as UNPUBLISHED")
    void unindexTitleMarksAsUnpublished() {
        when(searchDocumentRepository.findById(testId)).thenReturn(Optional.of(testDoc));

        searchService.unindexTitle(TitleUnpublishedEventPayload.builder()
                .titleId(testId)
                .unpublishedAt(Instant.now())
                .reason("Archived")
                .build());

        ArgumentCaptor<SearchDocument> captor = ArgumentCaptor.forClass(SearchDocument.class);
        verify(searchDocumentRepository).save(captor.capture());

        SearchDocument updated = captor.getValue();
        assertThat(updated.getPublicationState()).isEqualTo("UNPUBLISHED");
    }
}
