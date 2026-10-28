package com.vyroflix.content.repository;

import com.vyroflix.content.entity.ContentCollection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentCollectionRepository extends JpaRepository<ContentCollection, UUID> {

    /** Loads each collection's titles in the same query (no N+1 when rendering rails). */
    @EntityGraph(attributePaths = "titles")
    List<ContentCollection> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsBySlug(String slug);
}
