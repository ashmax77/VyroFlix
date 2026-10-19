package com.vyroflix.content.repository;

import com.vyroflix.content.entity.AssetType;
import com.vyroflix.content.entity.MediaAssetReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaAssetReferenceRepository extends JpaRepository<MediaAssetReference, UUID> {

    Optional<MediaAssetReference> findByTargetIdAndAssetType(UUID targetId, AssetType assetType);

    List<MediaAssetReference> findByTargetId(UUID targetId);
}
