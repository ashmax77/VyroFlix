package com.vyroflix.streaming.repository;

import com.vyroflix.streaming.entity.MasterPlaylistKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MasterPlaylistKeyRepository extends JpaRepository<MasterPlaylistKey, UUID> {
}
