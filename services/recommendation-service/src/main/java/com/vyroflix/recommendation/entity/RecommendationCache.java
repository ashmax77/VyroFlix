package com.vyroflix.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "recommendation_cache",
        schema = "recommendation",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_rail", columnNames = {"user_id", "rail_type"})
)
public class RecommendationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "rail_type", nullable = false, length = 50)
    private String railType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title_ids", columnDefinition = "jsonb", nullable = false)
    private String titleIdsJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
