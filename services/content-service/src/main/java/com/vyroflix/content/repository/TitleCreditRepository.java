package com.vyroflix.content.repository;

import com.vyroflix.content.entity.TitleCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TitleCreditRepository extends JpaRepository<TitleCredit, UUID> {

    List<TitleCredit> findByTitleId(UUID titleId);

    void deleteByTitleId(UUID titleId);
}
