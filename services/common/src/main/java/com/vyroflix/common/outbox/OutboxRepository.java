package com.vyroflix.common.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access repository for {@link OutboxEvent} records.
 *
 * <p>
 * Implemented directly using {@link EntityManager} to ensure compatibility
 * across all domain services without requiring separate Spring Data repository
 * scanning.
 * </p>
 */
public class OutboxRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public OutboxRepository() {
    }

    public OutboxRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Persists a new or merges an existing outbox event.
     *
     * @param event the outbox event
     * @return the saved outbox event
     */
    public OutboxEvent save(OutboxEvent event) {
        if (event.getId() == null) {
            entityManager.persist(event);
            return event;
        } else {
            return entityManager.merge(event);
        }
    }

    /**
     * Finds an outbox event by its primary key.
     *
     * @param id event UUID
     * @return optional containing the event if found
     */
    public Optional<OutboxEvent> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(OutboxEvent.class, id));
    }

    /**
     * Finds pending outbox events ordered by creation timestamp ascending.
     *
     * @param limit maximum records to fetch
     * @return list of pending outbox events
     */
    public List<OutboxEvent> findPendingEvents(int limit) {
        return entityManager.createQuery(
                "SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC",
                OutboxEvent.class)
                .setParameter("status", OutboxStatus.PENDING)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Purges published events older than the specified cutoff timestamp.
     *
     * @param status status filter (typically PUBLISHED)
     * @param cutoff cutoff timestamp
     * @return count of deleted records
     */
    public int deleteByStatusAndPublishedAtBefore(OutboxStatus status, Instant cutoff) {
        return entityManager.createQuery(
                "DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :cutoff")
                .setParameter("status", status)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}
