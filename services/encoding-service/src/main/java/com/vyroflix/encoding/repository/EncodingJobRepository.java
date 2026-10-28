package com.vyroflix.encoding.repository;

import com.vyroflix.encoding.entity.EncodingJob;
import com.vyroflix.encoding.entity.EncodingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncodingJobRepository extends JpaRepository<EncodingJob, UUID> {

    Optional<EncodingJob> findByUploadIntentId(UUID uploadIntentId);

    List<EncodingJob> findByContentId(UUID contentId);

    Page<EncodingJob> findByStatus(EncodingStatus status, Pageable pageable);

    @Query("SELECT j.status, COUNT(j) FROM EncodingJob j GROUP BY j.status")
    List<Object[]> countJobsByStatus();
}
