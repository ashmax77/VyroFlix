package com.vyroflix.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeFeedResponse {
    private TitleResponse featured;
    private List<CatalogRailResponse> rails;
}
