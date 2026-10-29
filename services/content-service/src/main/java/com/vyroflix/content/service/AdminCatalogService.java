package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.util.Constants;
import com.vyroflix.content.dto.*;
import com.vyroflix.content.entity.*;
import com.vyroflix.common.event.TitlePublishedEventPayload;
import com.vyroflix.common.event.EpisodePublishedEventPayload;
import com.vyroflix.common.event.TitleUnpublishedEventPayload;
import com.vyroflix.content.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final TitleRepository titleRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final MediaAssetReferenceRepository mediaAssetReferenceRepository;
    private final ObjectProvider<OutboxService> outboxServiceProvider;
    private final PersonRepository personRepository;
    private final TitleCreditRepository titleCreditRepository;

    @Transactional
    public TitleResponse createTitle(CreateTitleRequest request) {
        log.info("Creating new title: [{}] type={}", request.getTitle(), request.getType());

        Set<Genre> genres = new HashSet<>();
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            genres.addAll(genreRepository.findAllById(request.getGenreIds()));
        }

        Title title = Title.builder()
                .title(request.getTitle())
                .type(request.getType())
                .synopsis(request.getSynopsis())
                .releaseYear(request.getReleaseYear())
                .maturityRating(request.getMaturityRating())
                .thumbnailUrl(request.getThumbnailUrl())
                .posterUrl(request.getPosterUrl())
                .bannerUrl(request.getBannerUrl())
                .publicationState(PublicationState.DRAFT)
                .genres(genres)
                .build();

        Title saved = titleRepository.save(title);
        log.info("Title created with id={}", saved.getId());
        return toTitleResponse(saved);
    }

    @Transactional
    public TitleResponse updateTitle(UUID id, UpdateTitleRequest request) {
        Title title = titleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            title.setTitle(request.getTitle());
        }
        if (request.getSynopsis() != null) {
            title.setSynopsis(request.getSynopsis());
        }
        if (request.getReleaseYear() != null) {
            title.setReleaseYear(request.getReleaseYear());
        }
        if (request.getMaturityRating() != null) {
            title.setMaturityRating(request.getMaturityRating());
        }
        if (request.getThumbnailUrl() != null) {
            title.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getPosterUrl() != null) {
            title.setPosterUrl(request.getPosterUrl());
        }
        if (request.getBannerUrl() != null) {
            title.setBannerUrl(request.getBannerUrl());
        }
        if (request.getGenreIds() != null) {
            Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));
            title.setGenres(genres);
        }

        Title saved = titleRepository.save(title);
        if (saved.getPublicationState() == PublicationState.PUBLISHED) {
            publishCatalogEvent(Constants.TOPIC_CATALOG_TITLE_UPDATED, saved);
        }
        return toTitleResponse(saved);
    }

    @Transactional
    public SeasonResponse addSeason(UUID titleId, CreateSeasonRequest request) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + titleId));

        if (title.getType() != TitleType.SERIES) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Seasons can only be added to SERIES titles");
        }

        if (seasonRepository.findByTitleIdAndSeasonNumber(titleId, request.getSeasonNumber()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "Season number " + request.getSeasonNumber() + " already exists for this title");
        }

        Season season = Season.builder()
                .title(title)
                .seasonNumber(request.getSeasonNumber())
                .name(request.getName() != null ? request.getName() : "Season " + request.getSeasonNumber())
                .overview(request.getOverview())
                .build();

        Season saved = seasonRepository.save(season);
        return toSeasonResponse(saved);
    }

    @Transactional
    public EpisodeResponse addEpisode(UUID seasonId, CreateEpisodeRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Season not found with id: " + seasonId));

        if (episodeRepository.findBySeasonIdAndEpisodeNumber(seasonId, request.getEpisodeNumber()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "Episode number " + request.getEpisodeNumber() + " already exists in this season");
        }

        Episode episode = Episode.builder()
                .season(season)
                .episodeNumber(request.getEpisodeNumber())
                .name(request.getName())
                .overview(request.getOverview())
                .durationSeconds(request.getDurationSeconds())
                .thumbnailUrl(request.getThumbnailUrl())
                .publicationState(PublicationState.DRAFT)
                .build();

        Episode saved = episodeRepository.save(episode);
        return toEpisodeResponse(saved);
    }

    @Transactional
    public TitleResponse publishTitle(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + titleId));

        // Media readiness validation per AGENTS.md
        if (title.getType() == TitleType.MOVIE) {
            boolean ready = mediaAssetReferenceRepository
                    .findByTargetIdAndAssetType(titleId, AssetType.MAIN_VIDEO)
                    .map(asset -> asset.getStatus() == AssetStatus.READY)
                    .orElse(false);

            if (!ready) {
                log.warn("Cannot publish movie [{}]: main video media asset is not READY", titleId);
                throw new ApiException(ErrorCode.ASSET_NOT_READY, "Movie media asset is not READY for playback");
            }
        } else if (title.getType() == TitleType.SERIES) {
            List<Season> seasons = seasonRepository.findByTitleIdOrderBySeasonNumberAsc(titleId);
            if (seasons.isEmpty()) {
                throw new ApiException(ErrorCode.TITLE_NOT_PUBLISHABLE, "Cannot publish series without at least one season");
            }

            boolean anyEpisodeReady = false;
            for (Season season : seasons) {
                List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(season.getId());
                for (Episode episode : episodes) {
                    boolean ready = mediaAssetReferenceRepository
                            .findByTargetIdAndAssetType(episode.getId(), AssetType.MAIN_VIDEO)
                            .map(asset -> asset.getStatus() == AssetStatus.READY)
                            .orElse(false);
                    if (ready) {
                        anyEpisodeReady = true;
                        episode.setPublicationState(PublicationState.PUBLISHED);
                        episodeRepository.save(episode);
                        publishCatalogEpisodePublishedEvent(episode, titleId);
                    }
                }
            }

            if (!anyEpisodeReady) {
                throw new ApiException(ErrorCode.ASSET_NOT_READY, "Series has no episodes with READY media assets");
            }
        }

        title.setPublicationState(PublicationState.PUBLISHED);
        Title saved = titleRepository.save(title);

        publishCatalogEvent(Constants.TOPIC_CATALOG_TITLE_PUBLISHED, saved);
        log.info("Title published successfully: id={} title=[{}]", saved.getId(), saved.getTitle());
        return toTitleResponse(saved);
    }

    @Transactional
    public TitleResponse unpublishTitle(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + titleId));

        title.setPublicationState(PublicationState.UNPUBLISHED);
        Title saved = titleRepository.save(title);

        publishCatalogUnpublishedEvent(saved);
        log.info("Title unpublished: id={}", saved.getId());
        return toTitleResponse(saved);
    }

    /**
     * Updates or creates a READY media asset reference when video.encoded.v1 is received.
     */
    @Transactional
    public void updateMediaAssetReady(UUID targetId, String masterPlaylistUrl, Integer durationSeconds) {
        log.info("Updating media asset reference to READY for targetId [{}] masterPlaylistUrl [{}]",
                targetId, masterPlaylistUrl);

        MediaAssetReference asset = mediaAssetReferenceRepository
                .findByTargetIdAndAssetType(targetId, AssetType.MAIN_VIDEO)
                .orElseGet(() -> MediaAssetReference.builder()
                        .targetId(targetId)
                        .assetType(AssetType.MAIN_VIDEO)
                        .build());

        asset.setStatus(AssetStatus.READY);
        asset.setMasterPlaylistUrl(masterPlaylistUrl);
        asset.setDurationSeconds(durationSeconds);
        mediaAssetReferenceRepository.save(asset);

        log.info("MediaAssetReference updated to READY for targetId [{}]", targetId);
    }

    @Transactional(readOnly = true)
    public TitleResponse getTitle(UUID id) {
        Title title = titleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + id));
        return toTitleResponse(title);
    }

    @Transactional(readOnly = true)
    public List<TitleResponse> listTitles(PublicationState state, TitleType type) {
        List<Title> titles;
        if (state != null && type != null) {
            titles = titleRepository.findByPublicationStateAndType(state, type);
        } else if (state != null) {
            titles = titleRepository.findByPublicationState(state);
        } else {
            titles = titleRepository.findAll();
        }
        return titles.stream().map(this::toTitleResponse).collect(Collectors.toList());
    }

    @Transactional
    public GenreResponse createGenre(CreateGenreRequest request) {
        if (genreRepository.existsByName(request.getName())) {
            throw new ApiException(ErrorCode.CONFLICT, "Genre name already exists: " + request.getName());
        }
        if (genreRepository.existsBySlug(request.getSlug())) {
            throw new ApiException(ErrorCode.CONFLICT, "Genre slug already exists: " + request.getSlug());
        }

        Genre genre = Genre.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase().trim())
                .build();

        Genre saved = genreRepository.save(genre);
        return new GenreResponse(saved.getId(), saved.getName(), saved.getSlug());
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> listGenres() {
        return genreRepository.findAllByOrderByNameAsc().stream()
                .map(g -> new GenreResponse(g.getId(), g.getName(), g.getSlug()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MediaAssetReference linkMediaAsset(UUID targetId, AssetType assetType, String masterPlaylistUrl, Integer durationSeconds, AssetStatus status) {
        MediaAssetReference asset = mediaAssetReferenceRepository
                .findByTargetIdAndAssetType(targetId, assetType)
                .orElse(MediaAssetReference.builder()
                        .targetId(targetId)
                        .assetType(assetType)
                        .build());

        asset.setMasterPlaylistUrl(masterPlaylistUrl);
        asset.setDurationSeconds(durationSeconds);
        asset.setStatus(status);
        return mediaAssetReferenceRepository.save(asset);
    }

    /**
     * Re-emits catalog.title.updated.v1 for a title that is currently PUBLISHED (no-op otherwise), so search
     * and recommendation read models pick up credit or metadata changes.
     */
    @Transactional
    public void emitUpdatedIfPublished(UUID titleId) {
        titleRepository.findById(titleId)
                .filter(t -> t.getPublicationState() == PublicationState.PUBLISHED)
                .ifPresent(t -> publishCatalogEvent(Constants.TOPIC_CATALOG_TITLE_UPDATED, t));
    }

    /**
     * Deletes a title that is not published. Seasons, episodes, credits and collection links cascade in the database.
     */
    @Transactional
    public void deleteTitle(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + titleId));
        if (title.getPublicationState() == PublicationState.PUBLISHED) {
            throw new ApiException(ErrorCode.CONFLICT, "Unpublish the title before deleting it");
        }
        titleRepository.delete(title);
        log.info("Deleted title id={}", titleId);
    }

    @Transactional(readOnly = true)
    public CatalogStatsResponse catalogStats() {
        Map<String, Long> byType = new LinkedHashMap<>();
        for (TitleType type : TitleType.values()) {
            byType.put(type.name(), titleRepository.countByType(type));
        }
        Map<String, Long> byState = new LinkedHashMap<>();
        for (PublicationState state : PublicationState.values()) {
            byState.put(state.name(), titleRepository.countByPublicationState(state));
        }
        return CatalogStatsResponse.builder()
                .totalTitles(titleRepository.count())
                .byType(byType)
                .byPublicationState(byState)
                .build();
    }

    /**
     * Re-announces every published title, so read models fed by catalog events (search, recommendations, streaming
     * availability) catch up with titles that were seeded without events. Consumers upsert, so repeats are harmless.
     *
     * @return how many titles were announced
     */
    @Transactional
    public int republishPublishedTitles() {
        List<Title> published = titleRepository.findByPublicationState(PublicationState.PUBLISHED);
        published.forEach(title -> publishCatalogEvent(Constants.TOPIC_CATALOG_TITLE_PUBLISHED, title));
        return published.size();
    }

    private void publishCatalogEvent(String topic, Title title) {
        OutboxService outboxService = outboxServiceProvider.getIfAvailable();
        if (outboxService == null) {
            log.warn("OutboxService not available; skipping {} event", topic);
            return;
        }

        Set<String> genreSlugs = title.getGenres() != null
                ? title.getGenres().stream().map(Genre::getSlug).collect(Collectors.toSet())
                : Set.of();

        TitlePublishedEventPayload payload = TitlePublishedEventPayload.builder()
                .titleId(title.getId())
                .title(title.getTitle())
                .type(title.getType() != null ? title.getType().name() : "MOVIE")
                .synopsis(title.getSynopsis())
                .genreSlugs(genreSlugs)
                .releaseYear(title.getReleaseYear())
                .maturityRating(title.getMaturityRating())
                .posterUrl(title.getPosterUrl())
                .backdropUrl(title.getBannerUrl())
                .castMembers(creditNames(title.getId(), "ACTOR"))
                .directors(creditNames(title.getId(), "DIRECTOR"))
                .publishedAt(Instant.now())
                .build();

        outboxService.save(EventEnvelope.of(
                topic,
                Constants.SERVICE_CONTENT,
                UUID.randomUUID(),
                payload
        ));
    }

    /** Names for one credit role, in a single batched person lookup. */
    private List<String> creditNames(UUID titleId, String role) {
        List<TitleCredit> credits = titleCreditRepository.findByTitleId(titleId).stream()
                .filter(c -> role.equals(c.getRole()))
                .toList();
        if (credits.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> names = personRepository.findAllById(
                        credits.stream().map(TitleCredit::getPersonId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Person::getId, Person::getName));
        return credits.stream()
                .map(c -> names.get(c.getPersonId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void publishCatalogEpisodePublishedEvent(Episode episode, UUID titleId) {
        OutboxService outboxService = outboxServiceProvider.getIfAvailable();
        if (outboxService == null) {
            log.warn("OutboxService not available; skipping catalog.episode.published.v1 event");
            return;
        }

        EpisodePublishedEventPayload payload = EpisodePublishedEventPayload.builder()
                .episodeId(episode.getId())
                .titleId(titleId)
                .publishedAt(Instant.now())
                .build();

        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_CATALOG_EPISODE_PUBLISHED,
                Constants.SERVICE_CONTENT,
                UUID.randomUUID(),
                payload
        ));
    }

    private void publishCatalogUnpublishedEvent(Title title) {
        OutboxService outboxService = outboxServiceProvider.getIfAvailable();
        if (outboxService == null) return;

        TitleUnpublishedEventPayload payload = TitleUnpublishedEventPayload.builder()
                .titleId(title.getId())
                .unpublishedAt(Instant.now())
                .reason("UNPUBLISHED_BY_ADMIN")
                .build();

        outboxService.save(EventEnvelope.of(
                Constants.TOPIC_CATALOG_TITLE_UNPUBLISHED,
                Constants.SERVICE_CONTENT,
                UUID.randomUUID(),
                payload
        ));
    }

    private TitleResponse toTitleResponse(Title title) {
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
                    .map(this::toSeasonResponse)
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

    private SeasonResponse toSeasonResponse(Season season) {
        List<EpisodeResponse> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(season.getId()).stream()
                .map(this::toEpisodeResponse)
                .collect(Collectors.toList());

        return SeasonResponse.builder()
                .id(season.getId())
                .titleId(season.getTitle().getId())
                .seasonNumber(season.getSeasonNumber())
                .name(season.getName())
                .overview(season.getOverview())
                .episodes(episodes)
                .createdAt(season.getCreatedAt())
                .updatedAt(season.getUpdatedAt())
                .build();
    }

    private EpisodeResponse toEpisodeResponse(Episode episode) {
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
