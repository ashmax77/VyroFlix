package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.dto.*;
import com.vyroflix.content.entity.*;
import com.vyroflix.content.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final TitleRepository titleRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final MediaAssetReferenceRepository mediaAssetReferenceRepository;

    @Transactional(readOnly = true)
    public List<TitleResponse> listPublishedTitles(TitleType type, String genreSlug) {
        List<Title> titles;
        if (genreSlug != null && !genreSlug.isBlank()) {
            titles = titleRepository.findByGenreSlugAndState(genreSlug, PublicationState.PUBLISHED);
            if (type != null) {
                titles = titles.stream().filter(t -> t.getType() == type).collect(Collectors.toList());
            }
        } else if (type != null) {
            titles = titleRepository.findByPublicationStateAndType(PublicationState.PUBLISHED, type);
        } else {
            titles = titleRepository.findByPublicationState(PublicationState.PUBLISHED);
        }
        return titles.stream().map(this::toPublicTitleResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TitleResponse> pagePublishedTitles(Pageable pageable) {
        return titleRepository.findByPublicationState(PublicationState.PUBLISHED, pageable)
                .map(this::toPublicTitleResponse);
    }

    @Transactional(readOnly = true)
    public TitleResponse getPublishedTitle(UUID id) {
        Title title = titleRepository.findById(id)
                .filter(t -> t.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Published title not found with id: " + id));
        return toPublicTitleResponse(title);
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> listGenres() {
        return genreRepository.findAllByOrderByNameAsc().stream()
                .map(g -> new GenreResponse(g.getId(), g.getName(), g.getSlug()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HomeFeedResponse getHomeFeed() {
        List<Title> allPublished = titleRepository.findByPublicationState(PublicationState.PUBLISHED);

        TitleResponse featured = allPublished.isEmpty() ? null : toPublicTitleResponse(allPublished.get(0));

        List<CatalogRailResponse> rails = new ArrayList<>();

        // Trending / Top Movies rail
        List<TitleResponse> movies = allPublished.stream()
                .filter(t -> t.getType() == TitleType.MOVIE)
                .limit(10)
                .map(this::toPublicTitleResponse)
                .collect(Collectors.toList());
        if (!movies.isEmpty()) {
            rails.add(CatalogRailResponse.builder()
                    .id("trending-movies")
                    .title("Trending Movies")
                    .type("MOVIES")
                    .items(movies)
                    .build());
        }

        // Binge-Worthy Series rail
        List<TitleResponse> series = allPublished.stream()
                .filter(t -> t.getType() == TitleType.SERIES)
                .limit(10)
                .map(this::toPublicTitleResponse)
                .collect(Collectors.toList());
        if (!series.isEmpty()) {
            rails.add(CatalogRailResponse.builder()
                    .id("binge-series")
                    .title("Binge-Worthy Series")
                    .type("SERIES")
                    .items(series)
                    .build());
        }

        // Genre Rails
        List<Genre> genres = genreRepository.findAllByOrderByNameAsc();
        for (Genre genre : genres) {
            List<Title> genreTitles = titleRepository.findByGenreSlugAndState(genre.getSlug(), PublicationState.PUBLISHED);
            if (!genreTitles.isEmpty()) {
                rails.add(CatalogRailResponse.builder()
                        .id("genre-" + genre.getSlug())
                        .title(genre.getName())
                        .type("GENRE")
                        .items(genreTitles.stream().limit(10).map(this::toPublicTitleResponse).collect(Collectors.toList()))
                        .build());
            }
        }

        return HomeFeedResponse.builder()
                .featured(featured)
                .rails(rails)
                .build();
    }

    private TitleResponse toPublicTitleResponse(Title title) {
        boolean mediaReady = mediaAssetReferenceRepository
                .findByTargetIdAndAssetType(title.getId(), AssetType.MAIN_VIDEO)
                .map(a -> a.getStatus() == AssetStatus.READY)
                .orElse(false);

        Set<GenreResponse> genres = title.getGenres() != null
                ? title.getGenres().stream()
                .map(g -> new GenreResponse(g.getId(), g.getName(), g.getSlug()))
                .collect(Collectors.toSet())
                : Set.of();

        List<SeasonResponse> seasons = List.of();
        if (title.getType() == TitleType.SERIES) {
            seasons = seasonRepository.findByTitleIdOrderBySeasonNumberAsc(title.getId()).stream()
                    .map(this::toPublicSeasonResponse)
                    .collect(Collectors.toList());
        }

        return TitleResponse.builder()
                .id(title.getId())
                .title(title.getTitle())
                .type(title.getType())
                .synopsis(title.getSynopsis())
                .releaseYear(title.getReleaseYear())
                .maturityRating(title.getMaturityRating())
                .thumbnailUrl(title.getThumbnailUrl())
                .posterUrl(title.getPosterUrl())
                .bannerUrl(title.getBannerUrl())
                .publicationState(title.getPublicationState())
                .mediaReady(mediaReady)
                .genres(genres)
                .seasons(seasons)
                .createdAt(title.getCreatedAt())
                .updatedAt(title.getUpdatedAt())
                .build();
    }

    private SeasonResponse toPublicSeasonResponse(Season season) {
        List<EpisodeResponse> publishedEpisodes = episodeRepository
                .findBySeasonIdOrderByEpisodeNumberAsc(season.getId()).stream()
                .filter(ep -> ep.getPublicationState() == PublicationState.PUBLISHED)
                .map(this::toPublicEpisodeResponse)
                .collect(Collectors.toList());

        return SeasonResponse.builder()
                .id(season.getId())
                .titleId(season.getTitle().getId())
                .seasonNumber(season.getSeasonNumber())
                .name(season.getName())
                .overview(season.getOverview())
                .episodes(publishedEpisodes)
                .createdAt(season.getCreatedAt())
                .updatedAt(season.getUpdatedAt())
                .build();
    }

    private EpisodeResponse toPublicEpisodeResponse(Episode episode) {
        boolean mediaReady = mediaAssetReferenceRepository
                .findByTargetIdAndAssetType(episode.getId(), AssetType.MAIN_VIDEO)
                .map(a -> a.getStatus() == AssetStatus.READY)
                .orElse(false);

        return EpisodeResponse.builder()
                .id(episode.getId())
                .seasonId(episode.getSeason().getId())
                .episodeNumber(episode.getEpisodeNumber())
                .name(episode.getName())
                .overview(episode.getOverview())
                .durationSeconds(episode.getDurationSeconds())
                .thumbnailUrl(episode.getThumbnailUrl())
                .publicationState(episode.getPublicationState())
                .mediaReady(mediaReady)
                .createdAt(episode.getCreatedAt())
                .updatedAt(episode.getUpdatedAt())
                .build();
    }
}
