package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.dto.CollectionDtos.CollectionResponse;
import com.vyroflix.content.dto.CollectionDtos.CreateCollectionRequest;
import com.vyroflix.content.dto.CollectionDtos.TitleSummary;
import com.vyroflix.content.dto.CollectionDtos.UpdateCollectionTitlesRequest;
import com.vyroflix.content.entity.ContentCollection;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.Title;
import com.vyroflix.content.repository.ContentCollectionRepository;
import com.vyroflix.content.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final ContentCollectionRepository collectionRepository;
    private final TitleRepository titleRepository;

    /** Public rails: collections with at least one published title, showing only published titles. */
    @Transactional(readOnly = true)
    public List<CollectionResponse> listPublished() {
        return collectionRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> toResponse(c, c.getTitles().stream()
                        .filter(t -> t.getPublicationState() == PublicationState.PUBLISHED)))
                .filter(r -> !r.getTitles().isEmpty())
                .toList();
    }

    /** Admin view: every collection with all of its titles and their states. */
    @Transactional(readOnly = true)
    public List<CollectionResponse> listAll() {
        return collectionRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> toResponse(c, c.getTitles().stream()))
                .toList();
    }

    @Transactional
    public CollectionResponse create(CreateCollectionRequest request) {
        if (collectionRepository.existsBySlug(request.getSlug())) {
            throw new ApiException(ErrorCode.CONFLICT, "A collection with slug '" + request.getSlug() + "' already exists");
        }
        ContentCollection saved = collectionRepository.save(ContentCollection.builder()
                .name(request.getName().trim())
                .slug(request.getSlug())
                .displayOrder(request.getDisplayOrder())
                .build());
        log.info("Created collection id={} slug={}", saved.getId(), saved.getSlug());
        return toResponse(saved, saved.getTitles().stream());
    }

    /** Replaces the collection's titles. Every id must refer to an existing title. */
    @Transactional
    public CollectionResponse setTitles(UUID collectionId, UpdateCollectionTitlesRequest request) {
        ContentCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Collection not found with id: " + collectionId));

        Set<UUID> ids = new HashSet<>(request.getTitleIds());
        List<Title> titles = titleRepository.findAllById(ids);
        if (titles.size() != ids.size()) {
            throw new ApiException(ErrorCode.TITLE_NOT_FOUND, "One or more titles do not exist");
        }
        collection.setTitles(new HashSet<>(titles));
        collectionRepository.save(collection);
        return toResponse(collection, titles.stream());
    }

    private static CollectionResponse toResponse(ContentCollection collection, Stream<Title> titles) {
        List<TitleSummary> summaries = titles
                .sorted(Comparator.comparing(Title::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(t -> TitleSummary.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .type(t.getType() != null ? t.getType().name() : null)
                        .posterUrl(t.getPosterUrl())
                        .publicationState(t.getPublicationState() != null ? t.getPublicationState().name() : null)
                        .build())
                .collect(Collectors.toList());
        return CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .slug(collection.getSlug())
                .displayOrder(collection.getDisplayOrder())
                .titles(summaries)
                .build();
    }
}
