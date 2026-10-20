package com.vyroflix.search.controller;

import com.vyroflix.search.dto.SearchResultPage;
import com.vyroflix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResultPage> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        int boundedSize = Math.min(Math.max(size, 1), 50);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        log.debug("Executing search: query='{}', type='{}', page={}, size={}", query, type, boundedPage, boundedSize);
        SearchResultPage result = searchService.search(query, type, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(value = "q") String prefix,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        log.debug("Fetching search suggestions: prefix='{}', limit={}", prefix, limit);
        List<String> suggestions = searchService.getSuggestions(prefix, limit);
        return ResponseEntity.ok(suggestions);
    }
}
