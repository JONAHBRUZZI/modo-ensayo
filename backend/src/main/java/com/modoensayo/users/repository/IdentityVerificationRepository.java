package com.modoensayo.users.repository;

import com.modoensayo.users.domain.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Busca verificacion APROBADA con documento que coincida tras normalizar
     * (quitando puntos, guiones y espacios, en minusculas) en una cuenta DISTINTA.
     * Esto evita falsos negativos cuando el RUT se ingresa con/sin puntos.
     * Si retorna un resultado, hay conflicto con la cuenta que ya posee el documento.
     */
    @Query("""
        SELECT iv FROM IdentityVerification iv
        WHERE iv.status = 'APPROVED'
          AND iv.userId <> :userId
          AND LOWER(REPLACE(REPLACE(REPLACE(iv.documentNumber, '.', ''), '-', ''), ' ', '')) = :normalizedNumber
        """)
    Optional<IdentityVerification> findApprovedByNormalizedDocumentNumberExcludingUser(
            @Param("normalizedNumber") String normalizedNumber,
            @Param("userId") UUID userId);

    /**
     * Busca verificacion PENDING (en revision) con el mismo documento normalizado
     * en una cuenta distinta. Sirve para advertir cuando otra cuenta ya esta
     * solicitando validacion con ese mismo documento.
     */
    @Query("""
        SELECT iv FROM IdentityVerification iv
        WHERE iv.status = 'PENDING'
          AND iv.userId <> :userId
          AND LOWER(REPLACE(REPLACE(REPLACE(iv.documentNumber, '.', ''), '-', ''), ' ', '')) = :normalizedNumber
        """)
    Optional<IdentityVerification> findPendingByNormalizedDocumentNumberExcludingUser(
            @Param("normalizedNumber") String normalizedNumber,
            @Param("userId") UUID userId);
}
