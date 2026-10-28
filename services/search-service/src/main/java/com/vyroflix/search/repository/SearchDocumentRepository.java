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

    /**
     * Full-text search with optional filters. {@code likeQuery} is the user's text with LIKE wildcards escaped,
     * so a literal {@code %} or {@code _} in a title search matches itself. {@code genre} is a genre slug.
     */
    @Query(value = """
        SELECT * FROM search.search_documents
        WHERE publication_state = 'PUBLISHED'
          AND (
            CAST(:query AS text) IS NULL OR CAST(:query AS text) = '' OR
            search_vector @@ plainto_tsquery('english', CAST(:query AS text)) OR
            title ILIKE '%' || CAST(:likeQuery AS text) || '%' ESCAPE '\\'
          )
          AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text))
          AND (CAST(:genre AS text) IS NULL OR CAST(:genre AS text) = ANY(genres))
          AND (CAST(:year AS integer) IS NULL OR release_year = CAST(:year AS integer))
          AND (CAST(:maturity AS text) IS NULL OR maturity_rating = CAST(:maturity AS text))
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
            title ILIKE '%' || CAST(:likeQuery AS text) || '%' ESCAPE '\\'
          )
          AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text))
          AND (CAST(:genre AS text) IS NULL OR CAST(:genre AS text) = ANY(genres))
          AND (CAST(:year AS integer) IS NULL OR release_year = CAST(:year AS integer))
          AND (CAST(:maturity AS text) IS NULL OR maturity_rating = CAST(:maturity AS text))
        """,
        nativeQuery = true)
    Page<SearchDocument> filteredSearch(@Param("query") String query,
                                        @Param("likeQuery") String likeQuery,
                                        @Param("type") String type,
                                        @Param("genre") String genre,
                                        @Param("year") Integer year,
                                        @Param("maturity") String maturity,
                                        Pageable pageable);

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
