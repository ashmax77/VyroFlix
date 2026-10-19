package com.vyroflix.content.controller;

import com.vyroflix.content.dto.*;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminTitleController {

    private final AdminCatalogService adminCatalogService;

    // --- Titles ---

    @PostMapping("/titles")
    public ResponseEntity<TitleResponse> createTitle(@Valid @RequestBody CreateTitleRequest request) {
        TitleResponse response = adminCatalogService.createTitle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/titles")
    public ResponseEntity<List<TitleResponse>> listTitles(
            @RequestParam(value = "state", required = false) PublicationState state,
            @RequestParam(value = "type", required = false) TitleType type) {
        List<TitleResponse> titles = adminCatalogService.listTitles(state, type);
        return ResponseEntity.ok(titles);
    }

    @GetMapping("/titles/{id}")
    public ResponseEntity<TitleResponse> getTitle(@PathVariable("id") UUID id) {
        TitleResponse title = adminCatalogService.getTitle(id);
        return ResponseEntity.ok(title);
    }

    @PutMapping("/titles/{id}")
    public ResponseEntity<TitleResponse> updateTitle(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTitleRequest request) {
        TitleResponse response = adminCatalogService.updateTitle(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/titles/{id}/publish")
    public ResponseEntity<TitleResponse> publishTitle(@PathVariable("id") UUID id) {
        TitleResponse response = adminCatalogService.publishTitle(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/titles/{id}/unpublish")
    public ResponseEntity<TitleResponse> unpublishTitle(@PathVariable("id") UUID id) {
        TitleResponse response = adminCatalogService.unpublishTitle(id);
        return ResponseEntity.ok(response);
    }

    // --- Seasons & Episodes ---

    @PostMapping("/titles/{id}/seasons")
    public ResponseEntity<SeasonResponse> addSeason(
            @PathVariable("id") UUID titleId,
            @Valid @RequestBody CreateSeasonRequest request) {
        SeasonResponse response = adminCatalogService.addSeason(titleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<EpisodeResponse> addEpisode(
            @PathVariable("seasonId") UUID seasonId,
            @Valid @RequestBody CreateEpisodeRequest request) {
        EpisodeResponse response = adminCatalogService.addEpisode(seasonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- Genres ---

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> listGenres() {
        return ResponseEntity.ok(adminCatalogService.listGenres());
    }

    @PostMapping("/genres")
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody CreateGenreRequest request) {
        GenreResponse response = adminCatalogService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
