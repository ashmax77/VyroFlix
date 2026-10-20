package com.vyroflix.history.repository;

import com.vyroflix.history.entity.CompletionRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompletionRecordRepository extends JpaRepository<CompletionRecord, UUID> {

    Optional<CompletionRecord> findByUserIdAndContentId(UUID userId, UUID contentId);

    List<CompletionRecord> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndContentId(UUID userId, UUID contentId);
}
