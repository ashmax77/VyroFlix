package com.vyroflix.search.entity;

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
@Table(name = "search_documents", schema = "search")
public class SearchDocument {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // MOVIE, SERIES

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "maturity_rating", length = 10)
    private String maturityRating;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "genres", columnDefinition = "text[]")
    @Builder.Default
    private List<String> genres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cast_members", columnDefinition = "text[]")
    @Builder.Default
    private List<String> castMembers = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "directors", columnDefinition = "text[]")
    @Builder.Default
    private List<String> directors = new ArrayList<>();

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "publication_state", nullable = false, length = 20)
    @Builder.Default
    private String publicationState = "PUBLISHED";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
