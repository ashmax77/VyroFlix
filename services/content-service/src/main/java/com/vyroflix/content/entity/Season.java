package com.vyroflix.content.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seasons", schema = "content", uniqueConstraints = {
        @UniqueConstraint(name = "uq_title_season", columnNames = {"title_id", "season_number"})
})
public class Season extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("episodeNumber ASC")
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();
}
