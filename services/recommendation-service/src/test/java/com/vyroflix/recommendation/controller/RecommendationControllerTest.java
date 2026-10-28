package com.vyroflix.recommendation.controller;

import com.vyroflix.common.security.UserPrincipal;
import com.vyroflix.recommendation.dto.RecommendationItemResponse;
import com.vyroflix.recommendation.dto.RecommendationRailResponse;
import com.vyroflix.recommendation.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/trending returns trending rail")
    void getTrendingReturnsRail() throws Exception {
        UUID id = UUID.randomUUID();
        RecommendationItemResponse item = RecommendationItemResponse.builder()
                .titleId(id)
                .title("Interstellar")
                .type("MOVIE")
                .score(100.0)
                .reason("Trending #1")
                .build();

        RecommendationRailResponse rail = RecommendationRailResponse.builder()
                .railType("TRENDING_NOW")
                .title("Trending Now")
                .items(List.of(item))
                .build();

        when(recommendationService.getTrendingRail(anyInt())).thenReturn(rail);

        mockMvc.perform(get("/api/v1/recommendations/trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.railType").value("TRENDING_NOW"))
                .andExpect(jsonPath("$.items[0].title").value("Interstellar"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/similar/{titleId} returns similar rail")
    void getSimilarReturnsRail() throws Exception {
        UUID id = UUID.randomUUID();
        RecommendationRailResponse rail = RecommendationRailResponse.builder()
                .railType("SIMILAR_TITLES")
                .title("More Like This")
                .items(List.of())
                .build();

        when(recommendationService.getSimilarTitles(eq(id), anyInt())).thenReturn(rail);

        mockMvc.perform(get("/api/v1/recommendations/similar/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.railType").value("SIMILAR_TITLES"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/personal returns user personal recommendations")
    void getPersonalReturnsRail() throws Exception {
        UUID userId = UUID.randomUUID();
        RecommendationRailResponse rail = RecommendationRailResponse.builder()
                .railType("PERSONAL")
                .title("Recommended for You")
                .items(List.of())
                .build();

        when(recommendationService.getPersonalRecommendations(eq(userId), anyInt())).thenReturn(rail);

        mockMvc.perform(get("/api/v1/recommendations/personal")
                        .with(authentication(authAs(userId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.railType").value("PERSONAL"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/personal returns 401 without an authenticated user")
    void getPersonalRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/personal")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authAs(UUID userId) {
        UserPrincipal principal = UserPrincipal.builder().userId(userId).email("u@example.com").role("USER").build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }
}
