package com.vyroflix.encoding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hls_renditions", schema = "encoding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HlsRendition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encoding_job_id", nullable = false)
    private EncodingJob encodingJob;

    @Column(name = "resolution", nullable = false, length = 20)
    private String resolution;

    @Column(name = "bitrate", nullable = false)
    private int bitrate;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "playlist_key", nullable = false, length = 500)
    private String playlistKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
