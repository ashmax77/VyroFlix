package com.vyroflix.recommendation.repository;

import com.vyroflix.recommendation.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.Key> {

    /**
     * Atomically claims an event for a consumer.
     *
     * @return 1 if this call claimed it (first delivery), 0 if it was already processed
     */
    @Modifying
    @Query(value = """
            INSERT INTO recommendation.processed_events (event_id, consumer)
            VALUES (:eventId, :consumer)
            ON CONFLICT (event_id, consumer) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("eventId") String eventId, @Param("consumer") String consumer);
}
