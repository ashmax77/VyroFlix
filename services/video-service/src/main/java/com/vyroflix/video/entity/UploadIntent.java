package com.vyroflix.video.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_intents", schema = "video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadIntent extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UploadStatus status;

    @Column(name = "upload_url", columnDefinition = "TEXT")
    private String uploadUrl;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
