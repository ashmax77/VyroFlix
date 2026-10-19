package com.vyroflix.identity.repository;

import com.vyroflix.identity.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {

    Optional<DeviceSession> findByUserIdAndDeviceId(UUID userId, String deviceId);

    List<DeviceSession> findByUserId(UUID userId);
}
