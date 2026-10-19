package com.vyroflix.content.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "media_asset_references", schema = "content")
public class MediaAssetReference extends BaseEntity {

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssetStatus status = AssetStatus.PENDING;

    @Column(name = "master_playlist_url", length = 500)
    private String masterPlaylistUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
