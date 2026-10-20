package com.vyroflix.search.repository;

import com.vyroflix.search.entity.SearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SearchDocumentRepository extends JpaRepository<SearchDocument, UUID> {

    Optional<SearchDocument> findByIdAndPublicationState(UUID id, String publicationState);

    Page<SearchDocument> findByPublicationStateOrderByReleaseYearDesc(String publicationState, Pageable pageable);

    Page<SearchDocument> findByPublicationStateAndTypeOrderByReleaseYearDesc(String publicationState, String type, Pageable pageable);

    Page<SearchDocument> findByPublicationStateAndTitleContainingIgnoreCase(String publicationState, String title, Pageable pageable);

    @Query(value = """
        SELECT * FROM search.search_documents
        WHERE publication_state = 'PUBLISHED'
          AND (
            CAST(:query AS text) IS NULL OR CAST(:query AS text) = '' OR
            search_vector @@ plainto_tsquery('english', CAST(:query AS text)) OR
            title ILIKE '%' || CAST(:query AS text) || '%'
          )
          AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text))
        ORDER BY
          CASE WHEN CAST(:query AS text) IS NOT NULL AND CAST(:query AS text) != ''
               THEN ts_rank(search_vector, plainto_tsquery('english', CAST(:query AS text)))
               ELSE 0 END DESC,
          release_year DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM search.search_documents
        WHERE publication_state = 'PUBLISHED'
          AND (
            CAST(:query AS text) IS NULL OR CAST(:query AS text) = '' OR
            search_vector @@ plainto_tsquery('english', CAST(:query AS text)) OR
            title ILIKE '%' || CAST(:query AS text) || '%'
          )
          AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text))
        """,
        nativeQuery = true)
    Page<SearchDocument> fullTextSearch(@Param("query") String query, @Param("type") String type, Pageable pageable);

    @Query(value = """
        SELECT title FROM search.search_documents
        WHERE publication_state = 'PUBLISHED'
          AND title ILIKE :prefix || '%'
        ORDER BY title ASC
        LIMIT :limit
        """,
        nativeQuery = true)
    List<String> findSuggestions(@Param("prefix") String prefix, @Param("limit") int limit);
}
