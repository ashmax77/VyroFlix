package com.vyroflix.content.controller;

import com.vyroflix.content.dto.CollectionDtos.CollectionResponse;
import com.vyroflix.content.dto.CollectionDtos.CreateCollectionRequest;
import com.vyroflix.content.dto.CollectionDtos.UpdateCollectionTitlesRequest;
import com.vyroflix.content.service.CollectionService;
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
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
public class AdminCollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> list() {
        return ResponseEntity.ok(collectionService.listAll());
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> create(@Valid @RequestBody CreateCollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.create(request));
    }

    @PutMapping("/{id}/titles")
    public ResponseEntity<CollectionResponse> setTitles(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCollectionTitlesRequest request) {
        return ResponseEntity.ok(collectionService.setTitles(id, request));
    }
}
