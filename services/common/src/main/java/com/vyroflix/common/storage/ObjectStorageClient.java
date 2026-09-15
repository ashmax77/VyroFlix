package com.vyroflix.common.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction for object storage operations across VyroFlix.
 *
 * <p>Backed by AWS S3 SDK v2, compatible with both local MinIO and
 * production Cloudflare R2 / AWS S3.</p>
 *
 * <p>Per AGENTS.md: Video bytes must NOT pass through backend Spring Boot services
 * or API Gateway. Video uploads and playback must use short-lived presigned URLs.</p>
 */
public interface ObjectStorageClient {

    /**
     * Generates a pre-signed PUT URL for direct browser or client upload.
     *
     * @param bucket destination bucket (e.g. {@code vyroflix-raw})
     * @param key    object key path
     * @param expiry validity duration for the presigned URL
     * @return presigned upload URL
     */
    String generatePresignedUploadUrl(String bucket, String key, Duration expiry);

    /**
     * Generates a pre-signed PUT URL with a strict Content-Type requirement.
     *
     * @param bucket      destination bucket
     * @param key         object key path
     * @param expiry      validity duration
     * @param contentType expected media MIME type
     * @return presigned upload URL
     */
    String generatePresignedUploadUrl(String bucket, String key, Duration expiry, String contentType);

    /**
     * Generates a pre-signed GET URL for direct media playback via CDN / browser player.
     *
     * @param bucket source bucket (e.g. {@code vyroflix-content})
     * @param key    object key path (e.g. {@code titles/{id}/master.m3u8})
     * @param expiry validity duration for the signed manifest URL
     * @return presigned playback/download URL
     */
    String generatePresignedDownloadUrl(String bucket, String key, Duration expiry);

    /**
     * Directly uploads an object stream (used by Encoding-Service workers).
     *
     * @param bucket        destination bucket
     * @param key           object key path
     * @param data          input stream of object contents
     * @param contentLength size in bytes of the content
     * @param contentType   MIME type (e.g. {@code video/MP2T}, {@code application/x-mpegURL})
     */
    void putObject(String bucket, String key, InputStream data, long contentLength, String contentType);

    /**
     * Directly downloads an object stream (used by Encoding-Service workers).
     *
     * @param bucket source bucket
     * @param key    object key path
     * @return stream of object contents
     */
    InputStream getObject(String bucket, String key);

    /**
     * Deletes an object from the specified bucket.
     *
     * @param bucket target bucket
     * @param key    object key path
     */
    void deleteObject(String bucket, String key);

    /**
     * Verifies if an object exists in the specified bucket.
     *
     * @param bucket target bucket
     * @param key    object key path
     * @return true if object exists, false otherwise
     */
    boolean doesObjectExist(String bucket, String key);
}
