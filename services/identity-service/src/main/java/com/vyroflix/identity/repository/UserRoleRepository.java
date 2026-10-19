package com.vyroflix.identity.repository;

import com.vyroflix.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    boolean existsByUserIdAndRole(UUID userId, String role);
}
