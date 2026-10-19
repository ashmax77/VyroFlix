package com.vyroflix.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.content.dto.CreateGenreRequest;
import com.vyroflix.content.dto.CreateTitleRequest;
import com.vyroflix.content.dto.GenreResponse;
import com.vyroflix.content.dto.TitleResponse;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.service.AdminCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminTitleController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTitleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCatalogService adminCatalogService;

    @Test
    @DisplayName("POST /api/v1/admin/titles creates title and returns 201")
    void createTitle_Success() throws Exception {
        UUID id = UUID.randomUUID();
        CreateTitleRequest request = CreateTitleRequest.builder()
                .title("The Matrix")
                .type(TitleType.MOVIE)
                .synopsis("Welcome to the real world.")
                .build();

        TitleResponse response = TitleResponse.builder()
                .id(id)
                .title("The Matrix")
                .type(TitleType.MOVIE)
                .publicationState(PublicationState.DRAFT)
                .genres(Set.of())
                .seasons(List.of())
                .build();

        when(adminCatalogService.createTitle(any(CreateTitleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/titles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("The Matrix"))
                .andExpect(jsonPath("$.publicationState").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/titles/{id}/publish publishes title and returns 200")
    void publishTitle_Success() throws Exception {
        UUID id = UUID.randomUUID();
        TitleResponse response = TitleResponse.builder()
                .id(id)
                .title("The Matrix")
                .type(TitleType.MOVIE)
                .publicationState(PublicationState.PUBLISHED)
                .mediaReady(true)
                .genres(Set.of())
                .seasons(List.of())
                .build();

        when(adminCatalogService.publishTitle(id)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/titles/" + id + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/genres returns list of genres")
    void listGenres_Success() throws Exception {
        GenreResponse genre = new GenreResponse(UUID.randomUUID(), "Action", "action");
        when(adminCatalogService.listGenres()).thenReturn(List.of(genre));

        mockMvc.perform(get("/api/v1/admin/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[0].slug").value("action"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/genres creates genre and returns 201")
    void createGenre_Success() throws Exception {
        UUID id = UUID.randomUUID();
        CreateGenreRequest request = CreateGenreRequest.builder()
                .name("Sci-Fi")
                .slug("sci-fi")
                .build();

        GenreResponse response = new GenreResponse(id, "Sci-Fi", "sci-fi");
        when(adminCatalogService.createGenre(any(CreateGenreRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sci-Fi"));
    }
}
