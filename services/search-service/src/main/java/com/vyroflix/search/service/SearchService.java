package com.vyroflix.search.service;

import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.search.dto.SearchCardResponse;
import com.vyroflix.search.dto.SearchResultPage;
import com.vyroflix.search.entity.SearchDocument;
import com.vyroflix.search.repository.SearchDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchDocumentRepository searchDocumentRepository;

    @Transactional(readOnly = true)
    public SearchResultPage search(String query, String type, Pageable pageable) {
        return search(query, type, null, null, null, pageable);
    }

    /**
     * Search with optional genre slug, release year and maturity rating filters.
     * Without any filter the original keyword/type paths are used unchanged.
     */
    @Transactional(readOnly = true)
    public SearchResultPage search(String query, String type, String genre, Integer year, String maturity, Pageable pageable) {
        Page<SearchDocument> results;

        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanType = (type != null && !type.trim().isEmpty()) ? type.trim().toUpperCase() : null;
        String cleanGenre = (genre != null && !genre.trim().isEmpty()) ? genre.trim().toLowerCase() : null;
        String cleanMaturity = (maturity != null && !maturity.trim().isEmpty()) ? maturity.trim().toUpperCase() : null;

        boolean filtered = cleanGenre != null || year != null || cleanMaturity != null;
        if (filtered) {
            results = searchDocumentRepository.filteredSearch(
                    cleanQuery, escapeLike(cleanQuery), cleanType, cleanGenre, year, cleanMaturity, pageable);
        } else if (cleanQuery == null) {
            if (cleanType != null) {
                results = searchDocumentRepository.findByPublicationStateAndTypeOrderByReleaseYearDesc("PUBLISHED", cleanType, pageable);
            } else {
                results = searchDocumentRepository.findByPublicationStateOrderByReleaseYearDesc("PUBLISHED", pageable);
            }
        } else {
            results = searchDocumentRepository.fullTextSearch(cleanQuery, cleanType, pageable);
        }

        List<SearchCardResponse> cards = results.getContent().stream()
                .map(this::toSearchCard)
                .collect(Collectors.toList());

        return SearchResultPage.builder()
                .content(cards)
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .hasNext(results.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        int effectiveLimit = Math.min(Math.max(limit, 1), 10);
        return searchDocumentRepository.findSuggestions(query.trim(), effectiveLimit);
    }

    @Transactional
    public void indexTitle(TitlePublishedEventPayload payload) {
        log.info("Indexing published title: id={}, title={}", payload.getTitleId(), payload.getTitle());

        SearchDocument doc = searchDocumentRepository.findById(payload.getTitleId())
                .orElse(SearchDocument.builder()
                        .id(payload.getTitleId())
                        .createdAt(Instant.now())
                        .build());

        doc.setTitle(payload.getTitle());
        doc.setType(payload.getType() != null ? payload.getType() : "MOVIE");
        doc.setSynopsis(payload.getSynopsis());
        doc.setReleaseYear(payload.getReleaseYear());
        doc.setMaturityRating(payload.getMaturityRating());
        if (payload.getGenreSlugs() != null) {
            doc.setGenres(new ArrayList<>(payload.getGenreSlugs()));
        }
        if (payload.getCastMembers() != null) {
            doc.setCastMembers(new ArrayList<>(payload.getCastMembers()));
        }
        if (payload.getDirectors() != null) {
            doc.setDirectors(new ArrayList<>(payload.getDirectors()));
        }
        doc.setPosterUrl(payload.getPosterUrl());
        doc.setPublicationState("PUBLISHED");
        doc.setUpdatedAt(Instant.now());

        searchDocumentRepository.save(doc);
        log.info("Successfully indexed title: id={}", payload.getTitleId());
    }

    @Transactional
    public void unindexTitle(TitleUnpublishedEventPayload payload) {
        log.info("Evicting unpublished title from search index: id={}", payload.getTitleId());
        searchDocumentRepository.findById(payload.getTitleId()).ifPresentOrElse(doc -> {
            doc.setPublicationState("UNPUBLISHED");
            doc.setUpdatedAt(Instant.now());
            searchDocumentRepository.save(doc);
            log.info("Updated title publication state to UNPUBLISHED: id={}", payload.getTitleId());
        }, () -> log.debug("Title id={} not found in search index; nothing to unindex", payload.getTitleId()));
    }

    /** Escapes LIKE wildcards and the escape character itself, for use with ESCAPE '\'. */
    static String escapeLike(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private SearchCardResponse toSearchCard(SearchDocument doc) {
        return SearchCardResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .type(doc.getType())
                .synopsis(doc.getSynopsis())
                .releaseYear(doc.getReleaseYear())
                .maturityRating(doc.getMaturityRating())
                .genres(doc.getGenres())
                .castMembers(doc.getCastMembers())
                .directors(doc.getDirectors())
                .posterUrl(doc.getPosterUrl())
                .build();
    }
}
