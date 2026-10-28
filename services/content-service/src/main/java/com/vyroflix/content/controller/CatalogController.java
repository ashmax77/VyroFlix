package com.vyroflix.content.controller;

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
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

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
