package com.vyroflix.encoding.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "encoding_jobs", schema = "encoding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncodingJob extends BaseEntity {

    @Column(name = "upload_intent_id", nullable = false, unique = true)
    private UUID uploadIntentId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "raw_s3_bucket", nullable = false, length = 100)
    private String rawS3Bucket;

    @Column(name = "raw_s3_key", nullable = false, length = 500)
    private String rawS3Key;

    @Column(name = "output_s3_bucket", nullable = false, length = 100)
    private String outputS3Bucket;

    @Column(name = "output_s3_prefix", nullable = false, length = 500)
    private String outputS3Prefix;

    @Column(name = "master_playlist_key", length = 500)
    private String masterPlaylistKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EncodingStatus status = EncodingStatus.QUEUED;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @OneToMany(mappedBy = "encodingJob", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HlsRendition> renditions = new ArrayList<>();

    public void addRendition(HlsRendition rendition) {
        renditions.add(rendition);
        rendition.setEncodingJob(this);
    }
}
