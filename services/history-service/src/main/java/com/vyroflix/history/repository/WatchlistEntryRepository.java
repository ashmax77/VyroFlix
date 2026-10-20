package com.vyroflix.history.repository;

import com.vyroflix.history.entity.WatchlistEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, UUID> {

    Optional<WatchlistEntry> findByUserIdAndTitleId(UUID userId, UUID titleId);

    List<WatchlistEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndTitleId(UUID userId, UUID titleId);

    void deleteByUserIdAndTitleId(UUID userId, UUID titleId);
}
