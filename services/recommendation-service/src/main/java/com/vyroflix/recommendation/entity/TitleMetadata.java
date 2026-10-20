package com.vyroflix.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "title_metadata", schema = "recommendation")
public class TitleMetadata {

    @Id
    @Column(name = "title_id", nullable = false)
    private UUID titleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // MOVIE, SERIES

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "genres", columnDefinition = "text[]")
    @Builder.Default
    private List<String> genres = new ArrayList<>();

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "maturity_rating", length = 10)
    private String maturityRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
