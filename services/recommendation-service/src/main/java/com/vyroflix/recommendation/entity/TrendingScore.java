package com.vyroflix.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trending_scores", schema = "recommendation")
public class TrendingScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title_id", nullable = false, unique = true)
    private UUID titleId;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "completion_count", nullable = false)
    @Builder.Default
    private int completionCount = 0;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private double score = 0.0;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private Instant calculatedAt = Instant.now();
}
