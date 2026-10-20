package com.vyroflix.encoding.dto;

import com.vyroflix.encoding.entity.HlsRendition;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class HlsRenditionResponse {
    private final UUID id;
    private final String resolution;
    private final int bitrate;
    private final int width;
    private final int height;
    private final String playlistKey;

    public static HlsRenditionResponse from(HlsRendition rendition) {
        if (rendition == null) {
            return null;
        }
        return HlsRenditionResponse.builder()
                .id(rendition.getId())
                .resolution(rendition.getResolution())
                .bitrate(rendition.getBitrate())
                .width(rendition.getWidth())
                .height(rendition.getHeight())
                .playlistKey(rendition.getPlaylistKey())
                .build();
    }
}
