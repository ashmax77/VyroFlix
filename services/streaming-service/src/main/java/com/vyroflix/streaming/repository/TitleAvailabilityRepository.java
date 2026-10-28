package com.vyroflix.streaming.repository;

import com.vyroflix.streaming.entity.TitleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TitleAvailabilityRepository extends JpaRepository<TitleAvailability, UUID> {
}
