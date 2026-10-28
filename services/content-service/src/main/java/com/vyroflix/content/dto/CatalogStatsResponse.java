package com.vyroflix.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogStatsResponse {
    private long totalTitles;
    private Map<String, Long> byType;
    private Map<String, Long> byPublicationState;
}
