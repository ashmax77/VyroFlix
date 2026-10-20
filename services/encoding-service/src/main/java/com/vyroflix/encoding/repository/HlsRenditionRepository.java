package com.vyroflix.encoding.repository;

import com.vyroflix.encoding.entity.HlsRendition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HlsRenditionRepository extends JpaRepository<HlsRendition, UUID> {

    List<HlsRendition> findByEncodingJobId(UUID encodingJobId);
}
