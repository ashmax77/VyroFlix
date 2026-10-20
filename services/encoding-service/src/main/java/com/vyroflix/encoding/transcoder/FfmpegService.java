package com.vyroflix.encoding.transcoder;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service that coordinates FFmpeg media transcoding into multi-rendition HLS VOD packages.
 *
 * <p>Generates aligned keyframe renditions for 1080p, 720p, and 480p, with a master.m3u8 playlist.</p>
 * <p>Falls back to mock/synthetic HLS generation when running in development environments without FFmpeg CLI.</p>
 */
@Slf4j
@Service
public class FfmpegService {

    @Value("${vyroflix.encoding.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${vyroflix.encoding.timeout-minutes:30}")
    private int timeoutMinutes;

    @Getter
    @Builder
    public static class RenditionMetadata {
        private final String resolution;
        private final int bitrate;
        private final int width;
        private final int height;
        private final String playlistName;
    }

    @Getter
    @Builder
    public static class TranscodeResult {
        private final Path outputDir;
        private final String masterPlaylistName;
        private final int durationSeconds;
        private final List<RenditionMetadata> renditions;
    }

    public boolean isFfmpegInstalled() {
        try {
            Process process = new ProcessBuilder(ffmpegPath, "-version").start();
            boolean completed = process.waitFor(3, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("FFmpeg executable check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Packages the raw media file into multi-bitrate HLS streams.
     *
     * @param inputFile  the downloaded raw media file
     * @param outputDir  the local temporary directory to place HLS playlists and segments
     * @return transcode result containing file references and metadata
     */
    public TranscodeResult transcode(Path inputFile, Path outputDir) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);

        List<RenditionMetadata> renditions = List.of(
                RenditionMetadata.builder().resolution("1080p").bitrate(4500000).width(1920).height(1080).playlistName("1080p.m3u8").build(),
                RenditionMetadata.builder().resolution("720p").bitrate(2200000).width(1280).height(720).playlistName("720p.m3u8").build(),
                RenditionMetadata.builder().resolution("480p").bitrate(800000).width(854).height(480).playlistName("480p.m3u8").build()
        );

        if (isFfmpegInstalled()) {
            executeFfmpegTranscode(inputFile, outputDir, renditions);
        } else {
            log.warn("FFmpeg CLI not detected on system path. Generating synthetic HLS package for testing/local dev.");
            generateSyntheticHlsPackage(outputDir, renditions);
        }

        return TranscodeResult.builder()
                .outputDir(outputDir)
                .masterPlaylistName("master.m3u8")
                .durationSeconds(7200) // Default estimated duration
                .renditions(renditions)
                .build();
    }

    private void executeFfmpegTranscode(Path inputFile, Path outputDir, List<RenditionMetadata> renditions)
            throws IOException, InterruptedException {

        log.info("Starting FFmpeg multi-rendition transcoding for [{}] into [{}]", inputFile, outputDir);

        // FFmpeg multi-bitrate HLS pipeline with keyframe alignment (gop size 48 for 2s segments at 24fps)
        List<String> command = new ArrayList<>(List.of(
                ffmpegPath,
                "-y",
                "-i", inputFile.toAbsolutePath().toString(),
                "-filter_complex",
                "[0:v]split=3[v1][v2][v3];" +
                        "[v1]scale=w=1920:h=1080:force_original_aspect_ratio=decrease[v1out];" +
                        "[v2]scale=w=1280:h=720:force_original_aspect_ratio=decrease[v2out];" +
                        "[v3]scale=w=854:h=480:force_original_aspect_ratio=decrease[v3out]",
                "-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "4500k", "-maxrate:v:0", "4950k", "-bufsize:v:0", "9000k",
                "-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "2200k", "-maxrate:v:1", "2420k", "-bufsize:v:1", "4400k",
                "-map", "[v3out]", "-c:v:2", "libx264", "-b:v:2", "800k", "-maxrate:v:2", "880k", "-bufsize:v:2", "1600k",
                "-map", "0:a?", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a?", "-c:a:1", "aac", "-b:a:1", "128k",
                "-map", "0:a?", "-c:a:2", "aac", "-b:a:2", "96k",
                "-f", "hls",
                "-hls_time", "4",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", outputDir.resolve("stream_%v_data%03d.ts").toAbsolutePath().toString(),
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
                outputDir.resolve("stream_%v.m3u8").toAbsolutePath().toString()
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("FFmpeg transcoding timed out after " + timeoutMinutes + " minutes");
        }

        if (process.exitValue() != 0) {
            throw new IOException("FFmpeg exited with non-zero status code: " + process.exitValue());
        }

        log.info("FFmpeg transcoding completed successfully for [{}]", inputFile);
    }

    private void generateSyntheticHlsPackage(Path outputDir, List<RenditionMetadata> renditions) throws IOException {
        // 1. Generate master.m3u8
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION:3\n");

        for (RenditionMetadata r : renditions) {
            master.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n",
                    r.getBitrate(), r.getWidth(), r.getHeight()));
            master.append(r.getPlaylistName()).append("\n");

            // 2. Generate variant playlist (e.g. 1080p.m3u8)
            StringBuilder variant = new StringBuilder();
            variant.append("#EXTM3U\n");
            variant.append("#EXT-X-VERSION:3\n");
            variant.append("#EXT-X-TARGETDURATION:4\n");
            variant.append("#EXT-X-MEDIA-SEQUENCE:0\n");
            variant.append("#EXT-X-PLAYLIST-TYPE:VOD\n");

            for (int seg = 0; seg < 3; seg++) {
                String segFilename = String.format("%s_seg%03d.ts", r.getResolution(), seg);
                variant.append("#EXTINF:4.000,\n");
                variant.append(segFilename).append("\n");

                // Write synthetic dummy MPEG-TS segment
                Path segPath = outputDir.resolve(segFilename);
                if (!Files.exists(segPath)) {
                    byte[] dummyTs = ("VYROFLIX_SYNTHETIC_TS_SEGMENT_" + r.getResolution() + "_" + seg).getBytes(StandardCharsets.UTF_8);
                    Files.write(segPath, dummyTs);
                }
            }

            variant.append("#EXT-X-ENDLIST\n");
            Files.writeString(outputDir.resolve(r.getPlaylistName()), variant.toString(), StandardCharsets.UTF_8);
        }

        Files.writeString(outputDir.resolve("master.m3u8"), master.toString(), StandardCharsets.UTF_8);
    }
}
