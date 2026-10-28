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
@Table(name = "master_playlist_keys", schema = "streaming")
public class MasterPlaylistKey {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "playlist_key", nullable = false, length = 500)
    private String playlistKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
