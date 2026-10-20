package com.vyroflix.recommendation.repository;

import com.vyroflix.recommendation.entity.TitleMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TitleMetadataRepository extends JpaRepository<TitleMetadata, UUID> {

    Optional<TitleMetadata> findByTitleId(UUID titleId);

    List<TitleMetadata> findByTitleIdIn(Collection<UUID> titleIds);
}
