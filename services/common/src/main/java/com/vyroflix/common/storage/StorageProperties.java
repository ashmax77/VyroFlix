package com.vyroflix.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Configuration properties for S3-compatible object storage (MinIO / Cloudflare R2 / AWS S3).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vyroflix.storage")
public class StorageProperties {

    /**
     * Whether object storage is enabled. Defaults to true.
     */
    private boolean enabled = true;

    /**
     * Storage provider type: {@code MINIO} (local dev), {@code R2} (Cloudflare R2), or {@code S3} (AWS).
     */
    private Provider provider = Provider.MINIO;

    /**
     * S3-compatible service endpoint URI (e.g. {@code http://localhost:9000} or {@code https://<id>.r2.cloudflarestorage.com}).
     */
    private URI endpoint = URI.create("http://localhost:9000");

    /**
     * AWS region (e.g. {@code us-east-1} for MinIO, {@code auto} or {@code us-east-1} for R2).
     */
    private String region = "us-east-1";

    /**
     * S3 access key ID (MinIO root user or Cloudflare R2 API Token access key).
     */
    private String accessKey = "minioadmin";

    /**
     * S3 secret access key.
     */
    private String secretKey = "minioadmin";

    /**
     * Whether to use path-style access (e.g. {@code http://endpoint/bucket/key}).
     * Must be true for MinIO and typical R2 endpoints.
     */
    private boolean pathStyleAccessEnabled = true;

    /**
     * Bucket name for raw video uploads.
     */
    private String rawBucket = "vyroflix-raw";

    /**
     * Bucket name for encoded HLS master playlists and variant chunks.
     */
    private String contentBucket = "vyroflix-content";

    public enum Provider {
        MINIO,
        R2,
        S3
    }
}
