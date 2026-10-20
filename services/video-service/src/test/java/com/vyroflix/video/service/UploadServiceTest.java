package com.vyroflix.video.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import com.vyroflix.video.dto.CreateUploadIntentRequest;
import com.vyroflix.video.dto.UploadIntentResponse;
import com.vyroflix.video.entity.TargetType;
import com.vyroflix.video.entity.UploadIntent;
import com.vyroflix.video.entity.UploadStatus;
import com.vyroflix.video.repository.UploadIntentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private UploadIntentRepository uploadIntentRepository;

    @Mock
    private ObjectStorageClient storageClient;

    @Mock
    private OutboxService outboxService;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(uploadIntentRepository, storageClient, outboxService);
    }

    @Test
    @DisplayName("createUploadIntent creates intent with PENDING status and presigned PUT URL")
    void createUploadIntent_Success() {
        UUID contentId = UUID.randomUUID();
        CreateUploadIntentRequest request = CreateUploadIntentRequest.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .filename("masterpiece.mp4")
                .contentType("video/mp4")
                .fileSizeBytes(1024L * 1024L * 500L)
                .build();

        when(storageClient.generatePresignedUploadUrl(eq(Constants.BUCKET_RAW), anyString(), any(Duration.class), eq("video/mp4")))
                .thenReturn("https://r2.vyroflix.com/upload-presigned-url");

        when(uploadIntentRepository.save(any(UploadIntent.class))).thenAnswer(invocation -> {
            UploadIntent intent = invocation.getArgument(0);
            intent.setId(UUID.randomUUID());
            return intent;
        });

        UploadIntentResponse response = uploadService.createUploadIntent(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getContentId()).isEqualTo(contentId);
        assertThat(response.getStatus()).isEqualTo(UploadStatus.PENDING);
        assertThat(response.getUploadUrl()).isEqualTo("https://r2.vyroflix.com/upload-presigned-url");
        assertThat(response.getS3Bucket()).isEqualTo(Constants.BUCKET_RAW);
        assertThat(response.getS3Key()).startsWith("raw/movie/" + contentId);

        verify(uploadIntentRepository).save(any(UploadIntent.class));
    }

    @Test
    @DisplayName("completeUploadIntent verifies storage object, marks UPLOADED, and publishes outbox event")
    void completeUploadIntent_Success() {
        UUID intentId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UploadIntent pendingIntent = UploadIntent.builder()
                .contentId(contentId)
                .targetType(TargetType.MOVIE)
                .filename("film.mp4")
                .contentType("video/mp4")
                .fileSizeBytes(2000000L)
                .s3Bucket(Constants.BUCKET_RAW)
                .s3Key("raw/movie/" + contentId + "/film.mp4")
                .status(UploadStatus.PENDING)
                .build();
        pendingIntent.setId(intentId);

        when(uploadIntentRepository.findById(intentId)).thenReturn(Optional.of(pendingIntent));
        when(storageClient.doesObjectExist(Constants.BUCKET_RAW, pendingIntent.getS3Key())).thenReturn(true);
        when(uploadIntentRepository.save(any(UploadIntent.class))).thenAnswer(i -> i.getArgument(0));

        UploadIntentResponse response = uploadService.completeUploadIntent(intentId);

        assertThat(response.getStatus()).isEqualTo(UploadStatus.UPLOADED);
        verify(uploadIntentRepository).save(pendingIntent);

        ArgumentCaptor<EventEnvelope<?>> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).save(eventCaptor.capture());

        EventEnvelope<?> captured = eventCaptor.getValue();
        assertThat(captured.getEventType()).isEqualTo(Constants.TOPIC_VIDEO_UPLOADED);
        assertThat(captured.getProducer()).isEqualTo(Constants.SERVICE_VIDEO);
    }

    @Test
    @DisplayName("completeUploadIntent is idempotent if already marked as UPLOADED")
    void completeUploadIntent_AlreadyUploaded() {
        UUID intentId = UUID.randomUUID();
        UploadIntent uploadedIntent = UploadIntent.builder()
                .contentId(UUID.randomUUID())
                .targetType(TargetType.MOVIE)
                .status(UploadStatus.UPLOADED)
                .build();
        uploadedIntent.setId(intentId);

        when(uploadIntentRepository.findById(intentId)).thenReturn(Optional.of(uploadedIntent));

        UploadIntentResponse response = uploadService.completeUploadIntent(intentId);

        assertThat(response.getStatus()).isEqualTo(UploadStatus.UPLOADED);
        verify(storageClient, never()).doesObjectExist(anyString(), anyString());
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("completeUploadIntent fails with BAD_REQUEST if object missing in storage")
    void completeUploadIntent_ObjectNotFoundInStorage() {
        UUID intentId = UUID.randomUUID();
        UploadIntent pendingIntent = UploadIntent.builder()
                .contentId(UUID.randomUUID())
                .targetType(TargetType.MOVIE)
                .s3Bucket(Constants.BUCKET_RAW)
                .s3Key("raw/missing.mp4")
                .status(UploadStatus.PENDING)
                .build();
        pendingIntent.setId(intentId);

        when(uploadIntentRepository.findById(intentId)).thenReturn(Optional.of(pendingIntent));
        when(storageClient.doesObjectExist(Constants.BUCKET_RAW, "raw/missing.mp4")).thenReturn(false);

        assertThatThrownBy(() -> uploadService.completeUploadIntent(intentId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        assertThat(pendingIntent.getStatus()).isEqualTo(UploadStatus.FAILED);
        verify(uploadIntentRepository).save(pendingIntent);
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("completeUploadIntent throws NOT_FOUND for unknown intentId")
    void completeUploadIntent_NotFound() {
        UUID unknownId = UUID.randomUUID();
        when(uploadIntentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> uploadService.completeUploadIntent(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("getUploadIntent returns matching intent details")
    void getUploadIntent_Success() {
        UUID intentId = UUID.randomUUID();
        UploadIntent intent = UploadIntent.builder()
                .contentId(UUID.randomUUID())
                .targetType(TargetType.MOVIE)
                .filename("test.mp4")
                .status(UploadStatus.PENDING)
                .build();
        intent.setId(intentId);

        when(uploadIntentRepository.findById(intentId)).thenReturn(Optional.of(intent));

        UploadIntentResponse response = uploadService.getUploadIntent(intentId);
        assertThat(response.getId()).isEqualTo(intentId);
    }

    @Test
    @DisplayName("getIntentsByContentId returns all intents for content")
    void getIntentsByContentId_Success() {
        UUID contentId = UUID.randomUUID();
        UploadIntent intent1 = UploadIntent.builder().contentId(contentId).targetType(TargetType.MOVIE).build();
        intent1.setId(UUID.randomUUID());
        UploadIntent intent2 = UploadIntent.builder().contentId(contentId).targetType(TargetType.MOVIE).build();
        intent2.setId(UUID.randomUUID());

        when(uploadIntentRepository.findByContentId(contentId)).thenReturn(List.of(intent1, intent2));

        List<UploadIntentResponse> responses = uploadService.getIntentsByContentId(contentId);
        assertThat(responses).hasSize(2);
    }
}
