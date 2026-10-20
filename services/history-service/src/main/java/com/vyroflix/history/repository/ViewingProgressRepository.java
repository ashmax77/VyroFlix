package com.vyroflix.history.repository;

import com.vyroflix.history.entity.ViewingProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewingProgressRepository extends JpaRepository<ViewingProgress, UUID> {

    Optional<ViewingProgress> findByUserIdAndContentId(UUID userId, UUID contentId);

    List<ViewingProgress> findByUserIdAndIsCompletedFalseOrderByLastWatchedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserIdAndContentId(UUID userId, UUID contentId);
}
