package com.vyroflix.content.repository;

import com.vyroflix.content.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    List<Season> findByTitleIdOrderBySeasonNumberAsc(UUID titleId);

    Optional<Season> findByTitleIdAndSeasonNumber(UUID titleId, int seasonNumber);
}
