package com.vyroflix.encoding.service;

import com.vyroflix.common.event.EventEnvelope;
import com.vyroflix.common.event.VideoUploadedEventPayload;
import com.vyroflix.common.outbox.OutboxService;
import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import com.vyroflix.encoding.dto.EncodingJobResponse;
import com.vyroflix.encoding.entity.EncodingJob;
import com.vyroflix.encoding.entity.EncodingStatus;
import com.vyroflix.encoding.repository.EncodingJobRepository;
import com.vyroflix.encoding.transcoder.FfmpegService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncodingServiceTest {

    @Mock
    private EncodingJobRepository encodingJobRepository;

    @Mock
    private FfmpegService ffmpegService;

    @Mock
    private ObjectStorageClient storageClient;

    @Mock
    private OutboxService outboxService;

    private EncodingService encodingService;

    @BeforeEach
    void setUp() {
        encodingService = new EncodingService(encodingJobRepository, ffmpegService, storageClient, outboxService);
    }

    @Test
    @DisplayName("createOrGetJob creates a new job when one does not exist")
    void createOrGetJob_NewJob() {
        UUID uploadIntentId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        VideoUploadedEventPayload payload = VideoUploadedEventPayload.builder()
                .uploadIntentId(uploadIntentId)
                .contentId(contentId)
                .s3Bucket("vyroflix-raw")
                .s3Key("raw/movie/" + contentId + "/film.mp4")
                .build();

        when(encodingJobRepository.findByUploadIntentId(uploadIntentId)).thenReturn(Optional.empty());
        when(encodingJobRepository.save(any(EncodingJob.class))).thenAnswer(i -> {
            EncodingJob job = i.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        EncodingJob job = encodingService.createOrGetJob(payload);

        assertThat(job).isNotNull();
        assertThat(job.getUploadIntentId()).isEqualTo(uploadIntentId);
        assertThat(job.getContentId()).isEqualTo(contentId);
        assertThat(job.getStatus()).isEqualTo(EncodingStatus.QUEUED);
        assertThat(job.getOutputS3Bucket()).isEqualTo(Constants.BUCKET_CONTENT);
        assertThat(job.getOutputS3Prefix()).isEqualTo("titles/" + contentId + "/hls");

        verify(encodingJobRepository).save(any(EncodingJob.class));
    }

    @Test
    @DisplayName("createOrGetJob returns existing job idempotently")
    void createOrGetJob_ExistingJob() {
        UUID uploadIntentId = UUID.randomUUID();
        EncodingJob existing = EncodingJob.builder()
                .uploadIntentId(uploadIntentId)
                .contentId(UUID.randomUUID())
                .status(EncodingStatus.READY)
                .build();

        VideoUploadedEventPayload payload = VideoUploadedEventPayload.builder()
                .uploadIntentId(uploadIntentId)
                .build();

        when(encodingJobRepository.findByUploadIntentId(uploadIntentId)).thenReturn(Optional.of(existing));

        EncodingJob job = encodingService.createOrGetJob(payload);

        assertThat(job).isSameAs(existing);
        verify(encodingJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("executeEncoding completes transcoding, saves renditions, and publishes outbox event")
    void executeEncoding_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        EncodingJob job = EncodingJob.builder()
                .uploadIntentId(UUID.randomUUID())
                .contentId(contentId)
                .rawS3Bucket("vyroflix-raw")
                .rawS3Key("raw/video.mp4")
                .outputS3Bucket(Constants.BUCKET_CONTENT)
                .outputS3Prefix("titles/" + contentId + "/hls")
                .status(EncodingStatus.QUEUED)
                .build();
        job.setId(jobId);

        when(encodingJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(encodingJobRepository.save(any(EncodingJob.class))).thenAnswer(i -> i.getArgument(0));

        when(storageClient.getObject(anyString(), anyString()))
                .thenReturn(new ByteArrayInputStream("dummy-video-bytes".getBytes()));

        when(ffmpegService.transcode(any(Path.class), any(Path.class))).thenAnswer(invocation -> {
            Path outDir = invocation.getArgument(1);
            java.nio.file.Files.createDirectories(outDir);
            java.nio.file.Files.writeString(outDir.resolve("master.m3u8"), "#EXTM3U");
            return FfmpegService.TranscodeResult.builder()
                    .outputDir(outDir)
                    .masterPlaylistName("master.m3u8")
                    .durationSeconds(3600)
                    .renditions(List.of(
                            FfmpegService.RenditionMetadata.builder().resolution("1080p").bitrate(4500000).width(1920).height(1080).playlistName("1080p.m3u8").build(),
                            FfmpegService.RenditionMetadata.builder().resolution("720p").bitrate(2200000).width(1280).height(720).playlistName("720p.m3u8").build()
                    ))
                    .build();
        });

        EncodingJobResponse response = encodingService.executeEncoding(jobId);

        assertThat(response.getStatus()).isEqualTo(EncodingStatus.READY);
        assertThat(response.getMasterPlaylistKey()).isEqualTo("titles/" + contentId + "/hls/master.m3u8");
        assertThat(response.getDurationSeconds()).isEqualTo(3600);
        assertThat(response.getRenditions()).hasSize(2);

        ArgumentCaptor<EventEnvelope<?>> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outboxService).save(eventCaptor.capture());

        EventEnvelope<?> captured = eventCaptor.getValue();
        assertThat(captured.getEventType()).isEqualTo(Constants.TOPIC_VIDEO_ENCODED);
        assertThat(captured.getProducer()).isEqualTo(Constants.SERVICE_ENCODING);
    }

    @Test
    @DisplayName("executeEncoding handles transcode failure and marks job FAILED")
    void executeEncoding_Failure() throws Exception {
        UUID jobId = UUID.randomUUID();
        EncodingJob job = EncodingJob.builder()
                .uploadIntentId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .rawS3Bucket("vyroflix-raw")
                .rawS3Key("raw/corrupt.mp4")
                .outputS3Bucket(Constants.BUCKET_CONTENT)
                .outputS3Prefix("titles/hls")
                .status(EncodingStatus.QUEUED)
                .build();
        job.setId(jobId);

        when(encodingJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(encodingJobRepository.save(any(EncodingJob.class))).thenAnswer(i -> i.getArgument(0));

        when(ffmpegService.transcode(any(), any())).thenThrow(new RuntimeException("Corrupted video bitstream"));

        EncodingJobResponse response = encodingService.executeEncoding(jobId);

        assertThat(response.getStatus()).isEqualTo(EncodingStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("Corrupted video bitstream");
        verify(outboxService, never()).save(any());
    }

    @Test
    @DisplayName("retryJob increments retry count and resets status to QUEUED")
    void retryJob_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        EncodingJob job = EncodingJob.builder()
                .uploadIntentId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .rawS3Bucket("vyroflix-raw")
                .rawS3Key("raw/video.mp4")
                .outputS3Bucket(Constants.BUCKET_CONTENT)
                .outputS3Prefix("titles/hls")
                .status(EncodingStatus.FAILED)
                .retryCount(1)
                .build();
        job.setId(jobId);

        when(encodingJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(encodingJobRepository.save(any(EncodingJob.class))).thenAnswer(i -> i.getArgument(0));

        when(ffmpegService.transcode(any(Path.class), any(Path.class))).thenAnswer(invocation -> {
            Path outDir = invocation.getArgument(1);
            java.nio.file.Files.createDirectories(outDir);
            java.nio.file.Files.writeString(outDir.resolve("master.m3u8"), "#EXTM3U");
            return FfmpegService.TranscodeResult.builder()
                    .outputDir(outDir)
                    .masterPlaylistName("master.m3u8")
                    .durationSeconds(120)
                    .renditions(List.of())
                    .build();
        });

        EncodingJobResponse response = encodingService.retryJob(jobId);

        assertThat(response.getRetryCount()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(EncodingStatus.READY);
    }
}
