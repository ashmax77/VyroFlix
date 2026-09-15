package com.vyroflix.common.storage;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * S3-compatible implementation of {@link ObjectStorageClient}.
 *
 * <p>Supports MinIO, Cloudflare R2, and AWS S3 via AWS SDK v2.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorageClient implements ObjectStorageClient {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Override
    public String generatePresignedUploadUrl(String bucket, String key, Duration expiry) {
        return generatePresignedUploadUrl(bucket, key, expiry, null);
    }

    @Override
    public String generatePresignedUploadUrl(String bucket, String key, Duration expiry, String contentType) {
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);

            if (contentType != null && !contentType.isBlank()) {
                requestBuilder.contentType(contentType);
            }

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .putObjectRequest(requestBuilder.build())
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to generate presigned upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String bucket, String key, Duration expiry) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL for bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to generate presigned download URL: " + e.getMessage(), e);
        }
    }

    @Override
    public void putObject(String bucket, String key, InputStream data, long contentLength, String contentType) {
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);

            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }

            RequestBody requestBody = contentLength > 0
                    ? RequestBody.fromInputStream(data, contentLength)
                    : RequestBody.fromBytes(data.readAllBytes());

            s3Client.putObject(builder.build(), requestBody);
            log.debug("Successfully put object to bucket [{}] key [{}]", bucket, key);
        } catch (Exception e) {
            log.error("Failed to put object in bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to put object in storage: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (Exception e) {
            log.error("Failed to get object from bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to get object from storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.debug("Successfully deleted object from bucket [{}] key [{}]", bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete object from bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to delete object from storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean doesObjectExist(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check if object exists in bucket [{}] key [{}]: {}", bucket, key, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to verify object existence: " + e.getMessage(), e);
        }
    }
}
