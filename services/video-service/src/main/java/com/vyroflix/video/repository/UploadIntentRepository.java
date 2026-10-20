package com.vyroflix.video.repository;

import com.vyroflix.video.entity.UploadIntent;
import com.vyroflix.video.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadIntentRepository extends JpaRepository<UploadIntent, UUID> {

    List<UploadIntent> findByContentId(UUID contentId);

    List<UploadIntent> findByStatus(UploadStatus status);
}
