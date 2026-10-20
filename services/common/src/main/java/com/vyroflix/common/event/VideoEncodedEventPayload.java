package com.vyroflix.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload for {@code video.encoded.v1} event.
 *
 * <p>Emitted by Encoding-Service when multi-bitrate HLS packaging and upload to S3/R2 completes.
 * Consumed by Streaming-Service (caches master playlist key in Redis) and Content-Service
 * (transitions title media status to READY).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEventPayload {

    private UUID encodingJobId;
    private UUID uploadIntentId;
    private UUID contentId;
    private String s3Bucket;
    private String masterPlaylistKey;
    private Integer durationSeconds;
    private Instant encodedAt;
    private List<RenditionInfo> renditions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenditionInfo {
        private String resolution;
        private int bitrate;
        private int width;
        private int height;
        private String playlistKey;
    }
}
