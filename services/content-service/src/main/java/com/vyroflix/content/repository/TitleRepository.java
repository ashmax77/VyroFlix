package com.vyroflix.content.repository;

import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.Title;
import com.vyroflix.content.entity.TitleType;
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
public interface TitleRepository extends JpaRepository<Title, UUID> {

    List<Title> findByPublicationState(PublicationState publicationState);

    long countByType(TitleType type);

    long countByPublicationState(PublicationState publicationState);

    List<Title> findByPublicationStateAndType(PublicationState publicationState, TitleType type);

    Page<Title> findByPublicationState(PublicationState publicationState, Pageable pageable);

    @Query("SELECT t FROM Title t JOIN t.genres g WHERE g.slug = :slug AND t.publicationState = :state")
    List<Title> findByGenreSlugAndState(@Param("slug") String slug, @Param("state") PublicationState state);

    @Query("SELECT DISTINCT t FROM Title t LEFT JOIN FETCH t.genres WHERE t.id = :id")
    Optional<Title> findByIdWithGenres(@Param("id") UUID id);
}
