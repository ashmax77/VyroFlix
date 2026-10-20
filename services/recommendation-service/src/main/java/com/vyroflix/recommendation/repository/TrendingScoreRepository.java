package com.vyroflix.recommendation.repository;

import com.vyroflix.recommendation.entity.TrendingScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendingScoreRepository extends JpaRepository<TrendingScore, UUID> {

    Optional<TrendingScore> findByTitleId(UUID titleId);

    List<TrendingScore> findAllByOrderByScoreDesc(Pageable pageable);
}
