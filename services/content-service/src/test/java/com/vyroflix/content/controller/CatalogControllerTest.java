package com.vyroflix.content.controller;

import com.vyroflix.content.dto.CatalogRailResponse;
import com.vyroflix.content.dto.GenreResponse;
import com.vyroflix.content.dto.HomeFeedResponse;
import com.vyroflix.content.dto.TitleResponse;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @Test
    @DisplayName("GET /api/v1/catalog/home returns 200 with featured and rails")
    void getHomeFeed_Success() throws Exception {
        UUID titleId = UUID.randomUUID();
        TitleResponse featured = TitleResponse.builder()
                .id(titleId)
                .title("Interstellar")
                .type(TitleType.MOVIE)
                .publicationState(PublicationState.PUBLISHED)
                .mediaReady(true)
                .genres(Set.of())
                .seasons(List.of())
                .build();

        CatalogRailResponse rail = CatalogRailResponse.builder()
                .id("trending-movies")
                .title("Trending Movies")
                .type("MOVIES")
                .items(List.of(featured))
                .build();

        HomeFeedResponse homeFeed = HomeFeedResponse.builder()
                .featured(featured)
                .rails(List.of(rail))
                .build();

        when(catalogService.getHomeFeed()).thenReturn(homeFeed);

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.title").value("Interstellar"))
                .andExpect(jsonPath("$.rails[0].id").value("trending-movies"))
                .andExpect(jsonPath("$.rails[0].items[0].title").value("Interstellar"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/titles/{id} returns 200 with title")
    void getTitle_Success() throws Exception {
        UUID titleId = UUID.randomUUID();
        TitleResponse response = TitleResponse.builder()
                .id(titleId)
                .title("Inception")
                .type(TitleType.MOVIE)
                .publicationState(PublicationState.PUBLISHED)
                .mediaReady(true)
                .genres(Set.of())
                .seasons(List.of())
                .build();

        when(catalogService.getPublishedTitle(titleId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/catalog/titles/" + titleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(titleId.toString()))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/genres returns 200 with genre list")
    void listGenres_Success() throws Exception {
        GenreResponse genre = new GenreResponse(UUID.randomUUID(), "Sci-Fi", "sci-fi");
        when(catalogService.listGenres()).thenReturn(List.of(genre));

        mockMvc.perform(get("/api/v1/catalog/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sci-Fi"))
                .andExpect(jsonPath("$[0].slug").value("sci-fi"));
    }
}
