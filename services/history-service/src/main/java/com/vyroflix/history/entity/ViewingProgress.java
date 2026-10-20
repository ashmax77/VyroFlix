package com.vyroflix.history.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "viewing_progress", schema = "history", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_content_progress", columnNames = {"user_id", "content_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewingProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "position_seconds", nullable = false)
    private int positionSeconds;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "progress_percent", nullable = false)
    private double progressPercent;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "last_watched_at", nullable = false)
    private Instant lastWatchedAt;
}
