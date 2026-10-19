package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.content.dto.*;
import com.vyroflix.content.entity.*;
import com.vyroflix.content.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock
    private TitleRepository titleRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MediaAssetReferenceRepository mediaAssetReferenceRepository;

    @Mock
    private ObjectProvider<OutboxService> outboxServiceProvider;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AdminCatalogService adminCatalogService;

    private UUID titleId;

    @BeforeEach
    void setUp() {
        titleId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createTitle saves title in DRAFT publication state")
    void createTitle_Success() {
        CreateTitleRequest request = CreateTitleRequest.builder()
                .title("Inception")
                .type(TitleType.MOVIE)
                .synopsis("A dream within a dream.")
                .releaseYear(2010)
                .maturityRating("PG-13")
                .build();

        Title savedTitle = Title.builder()
                .title("Inception")
                .type(TitleType.MOVIE)
                .synopsis("A dream within a dream.")
                .releaseYear(2010)
                .maturityRating("PG-13")
                .publicationState(PublicationState.DRAFT)
                .genres(new HashSet<>())
                .seasons(new ArrayList<>())
                .build();
        savedTitle.setId(titleId);
        savedTitle.setCreatedAt(Instant.now());
        savedTitle.setUpdatedAt(Instant.now());

        when(titleRepository.save(any(Title.class))).thenReturn(savedTitle);

        TitleResponse response = adminCatalogService.createTitle(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(titleId);
        assertThat(response.getTitle()).isEqualTo("Inception");
        assertThat(response.getPublicationState()).isEqualTo(PublicationState.DRAFT);
        verify(titleRepository).save(any(Title.class));
    }

    @Test
    @DisplayName("addSeason succeeds for SERIES title")
    void addSeason_Success() {
        Title series = Title.builder()
                .title("Stranger Things")
                .type(TitleType.SERIES)
                .build();
        series.setId(titleId);

        when(titleRepository.findById(titleId)).thenReturn(Optional.of(series));
        when(seasonRepository.findByTitleIdAndSeasonNumber(titleId, 1)).thenReturn(Optional.empty());

        UUID seasonId = UUID.randomUUID();
        Season savedSeason = Season.builder()
                .title(series)
                .seasonNumber(1)
                .name("Season 1")
                .episodes(new ArrayList<>())
                .build();
        savedSeason.setId(seasonId);

        when(seasonRepository.save(any(Season.class))).thenReturn(savedSeason);

        CreateSeasonRequest request = CreateSeasonRequest.builder()
                .seasonNumber(1)
                .name("Season 1")
                .build();

        SeasonResponse response = adminCatalogService.addSeason(titleId, request);

        assertThat(response).isNotNull();
        assertThat(response.getSeasonNumber()).isEqualTo(1);
        assertThat(response.getTitleId()).isEqualTo(titleId);
    }

    @Test
    @DisplayName("addSeason throws BAD_REQUEST when adding season to a MOVIE title")
    void addSeason_ThrowsBadRequestForMovie() {
        Title movie = Title.builder()
                .title("Interstellar")
                .type(TitleType.MOVIE)
                .build();
        movie.setId(titleId);

        when(titleRepository.findById(titleId)).thenReturn(Optional.of(movie));

        CreateSeasonRequest request = CreateSeasonRequest.builder()
                .seasonNumber(1)
                .build();

        assertThatThrownBy(() -> adminCatalogService.addSeason(titleId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    @DisplayName("publishTitle throws ASSET_NOT_READY when movie media asset is missing or not ready")
    void publishTitle_Movie_ThrowsWhenMediaNotReady() {
        Title movie = Title.builder()
                .title("Dune")
                .type(TitleType.MOVIE)
                .publicationState(PublicationState.DRAFT)
                .build();
        movie.setId(titleId);

        when(titleRepository.findById(titleId)).thenReturn(Optional.of(movie));
        when(mediaAssetReferenceRepository.findByTargetIdAndAssetType(titleId, AssetType.MAIN_VIDEO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCatalogService.publishTitle(titleId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.ASSET_NOT_READY));
    }

    @Test
    @DisplayName("publishTitle succeeds when movie media asset is READY and emits outbox event")
    void publishTitle_Movie_SuccessWhenReady() {
        when(outboxServiceProvider.getIfAvailable()).thenReturn(outboxService);

        Title movie = Title.builder()
                .title("Dune")
                .type(TitleType.MOVIE)
                .genres(new HashSet<>())
                .seasons(new ArrayList<>())
                .publicationState(PublicationState.DRAFT)
                .build();
        movie.setId(titleId);

        MediaAssetReference mediaAsset = MediaAssetReference.builder()
                .targetId(titleId)
                .assetType(AssetType.MAIN_VIDEO)
                .status(AssetStatus.READY)
                .masterPlaylistUrl("s3://vyroflix-content/dune/master.m3u8")
                .build();

        when(titleRepository.findById(titleId)).thenReturn(Optional.of(movie));
        when(mediaAssetReferenceRepository.findByTargetIdAndAssetType(titleId, AssetType.MAIN_VIDEO))
                .thenReturn(Optional.of(mediaAsset));
        when(titleRepository.save(any(Title.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TitleResponse response = adminCatalogService.publishTitle(titleId);

        assertThat(response).isNotNull();
        assertThat(response.getPublicationState()).isEqualTo(PublicationState.PUBLISHED);
        verify(outboxService).save(any());
    }

    @Test
    @DisplayName("unpublishTitle sets publication state to UNPUBLISHED and emits outbox event")
    void unpublishTitle_Success() {
        when(outboxServiceProvider.getIfAvailable()).thenReturn(outboxService);

        Title title = Title.builder()
                .title("Test Movie")
                .type(TitleType.MOVIE)
                .genres(new HashSet<>())
                .seasons(new ArrayList<>())
                .publicationState(PublicationState.PUBLISHED)
                .build();
        title.setId(titleId);

        when(titleRepository.findById(titleId)).thenReturn(Optional.of(title));
        when(titleRepository.save(any(Title.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TitleResponse response = adminCatalogService.unpublishTitle(titleId);

        assertThat(response).isNotNull();
        assertThat(response.getPublicationState()).isEqualTo(PublicationState.UNPUBLISHED);
        verify(outboxService).save(any());
    }
}
