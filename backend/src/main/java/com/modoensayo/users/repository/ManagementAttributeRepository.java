package com.modoensayo.users.repository;

import com.modoensayo.users.domain.ManagementAttribute;
import com.modoensayo.users.domain.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagementAttributeRepository extends JpaRepository<ManagementAttribute, UUID> {
    List<ManagementAttribute> findByUser_Id(UUID userId);
    List<ManagementAttribute> findByUser_IdAndFechaFinAfter(UUID userId, LocalDateTime now);
    List<ManagementAttribute> findByUser_IdAndTipoPermisoAndFechaFinAfter(UUID userId, PermissionType tipo, LocalDateTime now);
    boolean existsByUser_IdAndTipoPermisoAndFechaFinAfter(UUID userId, PermissionType tipo, LocalDateTime now);
    long countByFechaFinBefore(LocalDateTime now);
    void deleteByFechaFinBefore(LocalDateTime now);
    List<ManagementAttribute> findByUser_IdAndTipoOrigen(UUID userId, String tipoOrigen);
    Optional<ManagementAttribute> findByUser_IdAndTargetIdAndFechaFinAfter(UUID userId, UUID targetId, LocalDateTime now);
    List<ManagementAttribute> findBySedeIdAndTipoOrigen(UUID sedeId, String tipoOrigen);
    List<ManagementAttribute> findByUser_IdAndTipoOrigenAndFechaFinAfter(UUID userId, String tipoOrigen, LocalDateTime now);
}
