package com.vyroflix.streaming.repository;

import com.vyroflix.streaming.entity.PlaybackSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaybackSessionRepository extends JpaRepository<PlaybackSession, UUID> {

    long countByUserIdAndEndedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    Optional<PlaybackSession> findByIdAndUserId(UUID id, UUID userId);
}
