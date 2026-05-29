package com.modoensayo.admin.service;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.repository.*;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final VenueRepository venueRepository;
    private final NotificationRepository notificationRepository;
    private final ClassRepository classRepository;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("usuarios", userRepository.count());
        stats.put("sedes", venueRepository.count());
        stats.put("pendientes", identityVerificationRepository.countByStatus("PENDING"));
        stats.put("sedesPendientes", venueRepository.countByStatus(EstadoSede.PENDIENTE_APROBACION));

        long totalClases = classRepository.count();
        long clasesRealizadas = classRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED).count();
        double ingresos = classRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .mapToDouble(c -> c.getPrice() != null ? c.getPrice() : 0).sum();
        long tasaOcupacion = totalClases > 0 ? (clasesRealizadas * 100 / totalClases) : 0;

        stats.put("totalClases", totalClases);
        stats.put("clasesRealizadas", clasesRealizadas);
        stats.put("tasaOcupacion", tasaOcupacion);
        stats.put("ingresos", ingresos);
        return stats;
    }

    public List<IdentityVerificationResponse> getIdentityVerifications() {
        return identityVerificationRepository.findByStatus("PENDING").stream()
                .map(iv -> new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(),
                        iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public IdentityVerificationResponse reviewIdentity(UUID id, String action, UUID reviewerId) {
        IdentityVerification iv = identityVerificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));
        iv.setStatus("approve".equals(action) ? "APPROVED" : "REJECTED");
        iv.setReviewedBy(reviewerId);
        iv = identityVerificationRepository.save(iv);

        if ("approve".equals(action)) {
            notificationRepository.save(Notification.builder()
                    .userId(iv.getUserId())
                    .message("Tu identidad ha sido VALIDADA. Ahora puedes registrar una sede o reservar salas para crear clases.")
                    .read(false).createdAt(Instant.now()).build());
        } else {
            notificationRepository.save(Notification.builder()
                    .userId(iv.getUserId())
                    .message("Tu solicitud de validacion de identidad ha sido RECHAZADA. Revisa tu documento y vuelve a intentarlo.")
                    .read(false).createdAt(Instant.now()).build());
        }

        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt());
    }

    public List<VenueResponse> getPendingVenues() {
        return venueRepository.findByStatusOrderByCreatedAtDesc(EstadoSede.PENDIENTE_APROBACION).stream()
                .map(v -> new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                        v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                        v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                        v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook()))
                .collect(Collectors.toList());
    }

    @Transactional
    public VenueResponse approveVenue(UUID id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        v.setStatus(EstadoSede.APROBADA);
        v = venueRepository.save(v);

        if (v.getAdminId() != null) {
            // Asignar rol VENUE_ADMIN automáticamente al aprobar la sede
            User owner = userRepository.findById(v.getAdminId()).orElse(null);
            if (owner != null) {
                Role venueAdminRole = roleRepository.findByName("VENUE_ADMIN").orElse(null);
                if (venueAdminRole != null) {
                    boolean hasRole = owner.getUserRoles().stream()
                            .anyMatch(ur -> ur.getRole().getName().equals("VENUE_ADMIN"));
                    if (!hasRole) {
                        UserRoleId uriId = new UserRoleId(owner.getId(), venueAdminRole.getId());
                        userRoleRepository.save(new UserRole(uriId, owner, venueAdminRole));
                    }
                }
                owner.setTieneSedeAprobada(true);
                userRepository.save(owner);
            }
            notificationRepository.save(Notification.builder()
                    .userId(v.getAdminId())
                    .message("Tu sede '" + v.getName() + "' ha sido APROBADA. Ya tienes acceso al panel de gestión de tu sede.")
                    .read(false).createdAt(Instant.now()).build());
        }

        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook());
    }

    @Transactional
    public VenueResponse rejectVenue(UUID id, String reason) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        v.setStatus(EstadoSede.RECHAZADA);
        v.setRejectionReason(reason);
        v = venueRepository.save(v);

        if (v.getAdminId() != null) {
            notificationRepository.save(Notification.builder()
                    .userId(v.getAdminId())
                    .message("Tu sede '" + v.getName() + "' ha sido RECHAZADA. Motivo: " + (reason != null ? reason : "No especificado") + ". Corrige los datos y reenvia.")
                    .read(false).createdAt(Instant.now()).build());
        }

        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook());
    }

    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("fullName", u.getFullName());
            map.put("enabled", u.isEnabled());
            map.put("roles", u.getUserRoles().stream().map(ur -> ur.getRole().getName()).collect(Collectors.toSet()));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void assignRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        boolean exists = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName().equals(roleName));
        if (!exists) {
            UserRole ur = UserRole.builder().user(user).role(role).build();
            userRoleRepository.save(ur);
        }
    }

    @Transactional
    public void revokeRole(UUID userId, String roleName) {
        userRoleRepository.findByUser_Id(userId).stream()
                .filter(ur -> ur.getRole().getName().equals(roleName))
                .findFirst()
                .ifPresent(userRoleRepository::delete);
    }

    @Transactional
    public void toggleUser(UUID userId, String motivo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);

        String msg = user.isEnabled()
                ? "Tu cuenta ha sido REACTIVADA."
                : "Tu cuenta ha sido SUSPENDIDA. Motivo: " + (motivo != null ? motivo : "No especificado") + ". Contacta al administrador.";
        notificationRepository.save(Notification.builder()
                .userId(userId).message(msg).read(false).createdAt(Instant.now()).build());
    }
}
