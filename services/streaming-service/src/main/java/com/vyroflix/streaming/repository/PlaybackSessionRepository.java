package com.vyroflix.streaming.repository;

import com.vyroflix.streaming.entity.PlaybackSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaybackSessionRepository extends JpaRepository<PlaybackSession, UUID> {

    long countByUserIdAndEndedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    long countByEndedAtIsNullAndExpiresAtAfter(Instant now);

    /** Sessions per UTC day since the given instant, as rows of [java.sql.Date day, Long count]. */
    @Query(value = """
        SELECT CAST(created_at AS date) AS day, COUNT(*) AS sessions
        FROM streaming.playback_sessions
        WHERE created_at >= :since
        GROUP BY CAST(created_at AS date)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> countSessionsPerDay(@Param("since") Instant since);

    Optional<PlaybackSession> findByIdAndUserId(UUID id, UUID userId);
}
