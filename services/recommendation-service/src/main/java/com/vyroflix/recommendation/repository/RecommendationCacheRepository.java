package com.vyroflix.recommendation.repository;

import com.vyroflix.recommendation.entity.RecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, UUID> {

    Optional<RecommendationCache> findByUserIdAndRailTypeAndExpiresAtAfter(UUID userId, String railType, Instant now);

    Optional<RecommendationCache> findByUserIdAndRailType(UUID userId, String railType);
}
