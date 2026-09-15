package com.vyroflix.common.storage;

import com.vyroflix.common.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageClientTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ObjectStorageClient client;

    @BeforeEach
    void setUp() {
        client = new S3ObjectStorageClient(s3Client, s3Presigner);
    }

    @Test
    @DisplayName("generatePresignedUploadUrl returns presigned URL string")
    void generatePresignedUploadUrl() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://storage.vyroflix.com/raw/test.mp4?signed=1").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        String url = client.generatePresignedUploadUrl("vyroflix-raw", "raw/test.mp4", Duration.ofMinutes(15));

        assertEquals("https://storage.vyroflix.com/raw/test.mp4?signed=1", url);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertEquals("vyroflix-raw", captor.getValue().putObjectRequest().bucket());
        assertEquals("raw/test.mp4", captor.getValue().putObjectRequest().key());
    }

    @Test
    @DisplayName("generatePresignedDownloadUrl returns presigned URL string")
    void generatePresignedDownloadUrl() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://storage.vyroflix.com/content/master.m3u8?signed=1").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = client.generatePresignedDownloadUrl("vyroflix-content", "content/master.m3u8", Duration.ofMinutes(10));

        assertEquals("https://storage.vyroflix.com/content/master.m3u8?signed=1", url);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertEquals("vyroflix-content", captor.getValue().getObjectRequest().bucket());
        assertEquals("content/master.m3u8", captor.getValue().getObjectRequest().key());
    }

    @Test
    @DisplayName("putObject uploads data stream to S3")
    void putObject() {
        byte[] bytes = "video chunk data".getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

        client.putObject("vyroflix-content", "1080p/chunk0.ts", stream, bytes.length, "video/MP2T");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("vyroflix-content", captor.getValue().bucket());
        assertEquals("1080p/chunk0.ts", captor.getValue().key());
        assertEquals("video/MP2T", captor.getValue().contentType());
    }

    @Test
    @DisplayName("getObject retrieves data stream from S3")
    void getObject() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(response, new ByteArrayInputStream("content".getBytes()));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        InputStream result = client.getObject("vyroflix-raw", "raw/test.mp4");

        assertNotNull(result);
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("deleteObject deletes object from bucket")
    void deleteObject() {
        client.deleteObject("vyroflix-content", "old-asset.m3u8");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("vyroflix-content", captor.getValue().bucket());
        assertEquals("old-asset.m3u8", captor.getValue().key());
    }

    @Test
    @DisplayName("doesObjectExist returns true when object exists and false on NoSuchKeyException")
    void doesObjectExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build())
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        assertTrue(client.doesObjectExist("vyroflix-content", "existing.m3u8"));
        assertFalse(client.doesObjectExist("vyroflix-content", "missing.m3u8"));
    }

    @Test
    @DisplayName("Exceptions are wrapped in ApiException")
    void exceptionWrapping() {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("S3 service unavailable"));

        assertThrows(ApiException.class, () ->
                client.generatePresignedUploadUrl("bucket", "key", Duration.ofMinutes(5)));
    }
}
