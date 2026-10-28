package com.vyroflix.encoding.controller;

import com.vyroflix.encoding.repository.EncodingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job counts by status for the admin processing dashboard. Restricted to ADMIN by the security chain
 * (/api/v1/encoding/**).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/encoding/stats")
@RequiredArgsConstructor
public class EncodingStatsController {

    private final EncodingJobRepository encodingJobRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Long>> jobsByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : encodingJobRepository.countJobsByStatus()) {
            counts.put(row[0].toString(), (Long) row[1]);
        }
        return ResponseEntity.ok(counts);
    }
}
