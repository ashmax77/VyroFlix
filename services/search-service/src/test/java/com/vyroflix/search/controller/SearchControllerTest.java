package com.vyroflix.search.controller;

import com.vyroflix.search.dto.SearchCardResponse;
import com.vyroflix.search.dto.SearchResultPage;
import com.vyroflix.search.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("GET /api/v1/search returns paginated search results")
    void searchEndpointReturnsResults() throws Exception {
        UUID id = UUID.randomUUID();
        SearchCardResponse card = SearchCardResponse.builder()
                .id(id)
                .title("Interstellar")
                .type("MOVIE")
                .releaseYear(2014)
                .maturityRating("PG-13")
                .genres(List.of("sci-fi"))
                .posterUrl("https://r2.vyroflix.com/posters/interstellar.jpg")
                .build();

        SearchResultPage page = SearchResultPage.builder()
                .content(List.of(card))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .build();

        when(searchService.search(eq("interstellar"), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "interstellar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Interstellar"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/search/suggestions returns suggestions list")
    void suggestionsEndpointReturnsList() throws Exception {
        when(searchService.getSuggestions(eq("inter"), eq(10)))
                .thenReturn(List.of("Interstellar"));

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("q", "inter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Interstellar"));
    }
}
