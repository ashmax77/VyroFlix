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
public class CatalogRailResponse {
    private String id;
    private String title;
    private String type;
    private List<TitleResponse> items;
}
