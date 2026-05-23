package com.modoensayo.users.repository;

import com.modoensayo.users.domain.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, UUID> {
    Optional<IdentityVerification> findByUserId(UUID userId);
    List<IdentityVerification> findByStatus(String status);
    long countByStatus(String status);
}
