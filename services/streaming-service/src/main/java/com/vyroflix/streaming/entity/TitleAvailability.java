package com.vyroflix.streaming.entity;

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
@Table(name = "title_availability", schema = "streaming")
public class TitleAvailability {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    /** Series id for episodes; null for movies and series themselves. */
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
