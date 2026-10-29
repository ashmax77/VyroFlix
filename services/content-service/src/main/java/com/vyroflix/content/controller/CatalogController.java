package com.vyroflix.content.controller;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.dto.CastMemberResponse;
import com.vyroflix.content.dto.CollectionDtos.CollectionResponse;
import com.vyroflix.content.dto.GenreResponse;
import com.vyroflix.content.dto.HomeFeedResponse;
import com.vyroflix.content.dto.TitleResponse;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.service.CastService;
import com.vyroflix.content.service.CatalogService;
import com.vyroflix.content.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private static final int MAX_BATCH_IDS = 50;

    private final CatalogService catalogService;
    private final CastService castService;
    private final CollectionService collectionService;

    @GetMapping("/home")
    public ResponseEntity<HomeFeedResponse> getHomeFeed() {
        HomeFeedResponse homeFeed = catalogService.getHomeFeed();
        return ResponseEntity.ok(homeFeed);
    }

    @GetMapping("/titles")
    public ResponseEntity<List<TitleResponse>> listTitles(
            @RequestParam(value = "type", required = false) TitleType type,
            @RequestParam(value = "genre", required = false) String genre) {
        List<TitleResponse> titles = catalogService.listPublishedTitles(type, genre);
        return ResponseEntity.ok(titles);
    }

    /** Several published titles in one call, for lists such as My list and Continue watching. */
    @GetMapping("/titles/batch")
    public ResponseEntity<List<TitleResponse>> getTitlesByIds(@RequestParam("ids") Set<UUID> ids) {
        if (ids.size() > MAX_BATCH_IDS) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Ask for at most " + MAX_BATCH_IDS + " titles at a time");
        }
        return ResponseEntity.ok(catalogService.listPublishedTitlesByIds(ids));
    }

    @GetMapping("/titles/{id}")
    public ResponseEntity<TitleResponse> getTitle(@PathVariable("id") UUID id) {
        TitleResponse title = catalogService.getPublishedTitle(id);
        List<CastMemberResponse> cast = castService.castFor(id);
        title.setCast(cast);
        return ResponseEntity.ok(title);
    }

    /** Curated rails. Only published titles are included; empty collections are omitted. */
    @GetMapping("/collections")
    public ResponseEntity<List<CollectionResponse>> listCollections() {
        return ResponseEntity.ok(collectionService.listPublished());
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> listGenres() {
        List<GenreResponse> genres = catalogService.listGenres();
        return ResponseEntity.ok(genres);
    }
}
