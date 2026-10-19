package com.vyroflix.content.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "episodes", schema = "content", uniqueConstraints = {
        @UniqueConstraint(name = "uq_season_episode", columnNames = {"season_id", "episode_number"})
})
public class Episode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private int episodeNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", nullable = false, length = 20)
    @Builder.Default
    private PublicationState publicationState = PublicationState.DRAFT;
}
