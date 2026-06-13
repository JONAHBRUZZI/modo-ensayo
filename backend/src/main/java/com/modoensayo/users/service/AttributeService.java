package com.modoensayo.users.service;

import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.domain.ManagementAttribute;
import com.modoensayo.users.domain.PermissionType;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.dto.AtributosActivosDTO;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.repository.ManagementAttributeRepository;
import com.modoensayo.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttributeService {

    private final UserRepository userRepository;
    private final ManagementAttributeRepository managementAttributeRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    public AttributeService(UserRepository userRepository,
                            ManagementAttributeRepository managementAttributeRepository,
                            IdentityVerificationRepository identityVerificationRepository) {
        this.userRepository = userRepository;
        this.managementAttributeRepository = managementAttributeRepository;
        this.identityVerificationRepository = identityVerificationRepository;
    }

    @Transactional
    public void promoverAMaestroIndependiente(String email, UUID salaId, LocalDateTime inicio, LocalDateTime fin) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ManagementAttribute attribute = new ManagementAttribute();
        attribute.setUser(user);
        attribute.setTargetId(salaId);
        attribute.setTipoPermiso(PermissionType.GESTION_SALA);
        attribute.setTipoOrigen("RESERVA_PROPIA");
        attribute.setFechaInicio(inicio);
        attribute.setFechaFin(fin);
        managementAttributeRepository.save(attribute);
    }

    @Transactional
    public void asignarProfesorDependiente(String email, UUID claseId, UUID sedeId, LocalDateTime inicio, LocalDateTime fin) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ManagementAttribute attribute = new ManagementAttribute();
        attribute.setUser(user);
        attribute.setTargetId(claseId);
        attribute.setTipoPermiso(PermissionType.CLASE_ASIGNADA);
        attribute.setTipoOrigen("ASIGNACION_SEDE");
        attribute.setSedeId(sedeId);
        attribute.setFechaInicio(inicio);
        attribute.setFechaFin(fin);
        managementAttributeRepository.save(attribute);
    }

    @Transactional
    public void revocarAtributo(UUID atributoId) {
        if (!managementAttributeRepository.existsById(atributoId)) {
            throw new ResourceNotFoundException("Attribute not found");
        }
        managementAttributeRepository.deleteById(atributoId);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveRoles(User user) {
        List<String> roles = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<ManagementAttribute> activeAttributes = managementAttributeRepository
                .findByUser_IdAndFechaFinAfter(user.getId(), now);

        for (ManagementAttribute attr : activeAttributes) {
            roles.add(attr.getTipoPermiso().name());
        }

        return roles;
    }

    @Transactional(readOnly = true)
    public AtributosActivosDTO computeAtributosActivos(User user) {
        AtributosActivosDTO dto = new AtributosActivosDTO();
        LocalDateTime now = LocalDateTime.now();

        IdentityVerification verification = identityVerificationRepository
                .findByUserId(user.getId()).orElse(null);
        if (verification != null) {
            dto.setIdentidadValidada("APPROVED".equals(verification.getStatus()));
            dto.setIdentidadPendiente("PENDING".equals(verification.getStatus()));
        }

        boolean tieneReservas = managementAttributeRepository
                .existsByUser_IdAndTipoPermisoAndFechaFinAfter(user.getId(), PermissionType.GESTION_SALA, now);
        dto.setTieneReservasActivas(tieneReservas);

        boolean tieneAsignaciones = managementAttributeRepository
                .existsByUser_IdAndTipoPermisoAndFechaFinAfter(user.getId(), PermissionType.CLASE_ASIGNADA, now);
        dto.setTieneAsignacionesActivas(tieneAsignaciones);

        List<ManagementAttribute> gestionSedeAttrs = managementAttributeRepository
                .findByUser_IdAndTipoPermisoAndFechaFinAfter(user.getId(), PermissionType.GESTION_SEDE, now);
        if (!gestionSedeAttrs.isEmpty()) {
            dto.setEsAdminSede(true);
            dto.setSedeId(gestionSedeAttrs.get(0).getSedeId());
        }

        boolean esAdmin = managementAttributeRepository
                .existsByUser_IdAndTipoPermisoAndFechaFinAfter(user.getId(), PermissionType.ROLE_ADMIN, now);
        dto.setEsAdminGeneral(esAdmin);

        dto.setEstadoUsuario("ACTIVO");

        return dto;
    }
}
