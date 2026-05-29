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

    /**
     * Verifica si existe un documento con el mismo número ya aprobado en OTRA cuenta.
     * Previene que el mismo RUT/Pasaporte sea validado en múltiples usuarios.
     */
    boolean existsByDocumentNumberAndStatusAndUserIdNot(String documentNumber, String status, UUID userId);
}
