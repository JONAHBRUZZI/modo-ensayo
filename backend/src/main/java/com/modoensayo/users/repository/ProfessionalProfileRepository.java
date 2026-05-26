package com.modoensayo.users.repository;

import com.modoensayo.users.domain.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, UUID> {
    Optional<ProfessionalProfile> findByUser_Id(UUID userId);
}
