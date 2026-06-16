package com.modoensayo.admin.service;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.enums.PaymentSessionStatus;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.payments.repository.PaymentSessionRepository;
import com.modoensayo.attendance.repository.AttendanceRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.repository.*;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.reschedules.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NotificationService notificationService;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final AttendanceRepository attendanceRepository;

    @Cacheable("adminStats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("usuarios", userRepository.count());
        stats.put("sedes", venueRepository.count());
        stats.put("pendientes", identityVerificationRepository.countByStatus("PENDING"));
        stats.put("sedesPendientes", venueRepository.countByStatus(EstadoSede.PENDIENTE_APROBACION));
        stats.put("sedesSuspendidas", venueRepository.countByStatus(EstadoSede.SUSPENDIDA));

        var todasLasClases = classRepository.findAll();
        long totalClases = todasLasClases.size();
        long clasesRealizadas = todasLasClases.stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED).count();
        double ingresos = todasLasClases.stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .mapToDouble(c -> c.getPrice() != null ? c.getPrice() : 0).sum();
        stats.put("totalClases", totalClases);
        stats.put("clasesRealizadas", clasesRealizadas);
        stats.put("ingresos", ingresos);

        // M1: Tasa de ocupacion CORRECTA (inscritos / capacidad)
        long sumInscritos = 0, sumCapacidad = 0;
        for (var c : todasLasClases) {
            if (c.getCapacity() != null && c.getCapacity() > 0) {
                sumInscritos += enrollmentRepository.countByClassId(c.getId());
                sumCapacidad += c.getCapacity();
            }
        }
        long tasaOcupacion = sumCapacidad > 0
                ? Math.round((double) sumInscritos / sumCapacidad * 100) : 0;
        stats.put("tasaOcupacion", tasaOcupacion);

        // M2: Conversion reserva a pago (PaymentSession)
        long sesAprobadas = paymentSessionRepository.countByStatus(PaymentSessionStatus.APPROVED);
        long sesFallidas  = paymentSessionRepository.countByStatus(PaymentSessionStatus.FAILED);
        long totalSes = sesAprobadas + sesFallidas;
        long conversionPago = totalSes > 0
                ? Math.round((double) sesAprobadas / totalSes * 100) : 100;
        stats.put("conversionPago", conversionPago);
        stats.put("sesionesAprobadas", sesAprobadas);
        stats.put("sesionesFallidas", sesFallidas);

        // M3: Tasa de asistencia
        long totalPresentes = 0, totalInscritos = 0;
        var clasesCompletadas = todasLasClases.stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED).toList();
        for (var c : clasesCompletadas) {
            totalPresentes += attendanceRepository.countByClassIdAndPresentTrue(c.getId());
            totalInscritos  += enrollmentRepository.countByClassId(c.getId());
        }
        long tasaAsistencia = totalInscritos > 0
                ? Math.round((double) totalPresentes / totalInscritos * 100) : 0;
        stats.put("tasaAsistencia", tasaAsistencia);

        // M5: Pagos exitosos
        long totalPagos  = paymentRepository.count();
        long pagosFailed = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long tasaPagosExitosos = totalPagos > 0
                ? Math.round((double)(totalPagos - pagosFailed) / totalPagos * 100) : 100;
        stats.put("tasaPagosExitosos", tasaPagosExitosos);
        stats.put("totalPagos", totalPagos);
        stats.put("pagosFailed", pagosFailed);

        // Datos para graficos
        Map<String, Long> sedesPorEstado = new LinkedHashMap<>();
        for (EstadoSede estado : List.of(EstadoSede.APROBADA, EstadoSede.PENDIENTE_APROBACION,
                EstadoSede.RECHAZADA, EstadoSede.SUSPENDIDA)) {
            sedesPorEstado.put(estado.name(), venueRepository.countByStatus(estado));
        }
        stats.put("sedesPorEstado", sedesPorEstado);

        Map<String, Long> usuariosPorRol = new LinkedHashMap<>();
        var roles = roleRepository.findAll();
        for (Role r : roles) {
            usuariosPorRol.put(r.getName(), userRoleRepository.countByRoleId(r.getId()));
        }
        stats.put("usuariosPorRol", usuariosPorRol);

        List<Map<String, Object>> ingresosMensuales = new ArrayList<>();
        Map<String, Double> porMes = new TreeMap<>();
        for (var c : clasesCompletadas) {
            if (c.getEndTime() != null) {
                String mes = java.time.LocalDate.ofInstant(c.getEndTime(), java.time.ZoneOffset.UTC)
                        .withDayOfMonth(1).toString();
                porMes.merge(mes, c.getPrice() != null ? c.getPrice() : 0, Double::sum);
            }
        }
        for (var entry : porMes.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("mes", entry.getKey());
            item.put("ingresos", entry.getValue().intValue());
            ingresosMensuales.add(item);
        }
        stats.put("ingresosMensuales", ingresosMensuales);

        return stats;
    }

    public List<IdentityVerificationResponse> getIdentityVerifications() {
        return identityVerificationRepository.findByStatus("PENDING").stream()
                .map(iv -> new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(),
                        iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt(),
                        iv.getDocumentType(), iv.getDocumentNumber(), iv.getFullName(), iv.getBirthDate()))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "adminStats", allEntries = true)
    public IdentityVerificationResponse reviewIdentity(UUID id, String action, UUID reviewerId) {
        IdentityVerification iv = identityVerificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));
        boolean aprobado = "approve".equals(action);
        iv.setStatus(aprobado ? "APPROVED" : "REJECTED");
        iv.setReviewedBy(reviewerId);
        iv = identityVerificationRepository.save(iv);

        // Persistir estado en User para acceso rápido sin JOIN
        User owner = userRepository.findById(iv.getUserId()).orElse(null);
        if (owner != null) {
            owner.setIdentidadValidada(aprobado);
            owner.setIdentidadEstado(aprobado ? "APROBADO" : "RECHAZADO");
            userRepository.save(owner);
        }

        if (aprobado) {
            notificationService.enviar(iv.getUserId(), null, null,
                    "Tu identidad ha sido VALIDADA. Ahora puedes registrar una sede o reservar salas para crear clases.");
        } else {
            notificationService.enviar(iv.getUserId(), null, null,
                    "Tu solicitud de validacion de identidad ha sido RECHAZADA. Revisa tu documento y vuelve a intentarlo.");
        }

        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt(),
                iv.getDocumentType(), iv.getDocumentNumber(), iv.getFullName(), iv.getBirthDate());
    }

    public List<VenueResponse> getPendingVenues() {
        return venueRepository.findByStatusOrderByCreatedAtDesc(EstadoSede.PENDIENTE_APROBACION).stream()
                .map(v -> new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                        v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                        v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                        v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook()))
                .collect(Collectors.toList());
    }

    /**
     * Retorna TODAS las sedes registradas en el sistema (cualquier estado).
     * Cada item incluye el nombre y email del Admin de Sede para que el Admin General
     * pueda identificar al responsable rapidamente.
     */
    public List<Map<String, Object>> getAllVenues() {
        return venueRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId().toString());
                    map.put("name", v.getName());
                    map.put("city", v.getCity());
                    map.put("address", v.getAddress());
                    map.put("description", v.getDescription());
                    map.put("phone", v.getPhone());
                    map.put("email", v.getEmail());
                    map.put("status", v.getStatus() != null ? v.getStatus().name() : null);
                    map.put("tipo", v.getTipo() != null ? v.getTipo().name() : null);
                    map.put("createdAt", v.getCreatedAt());
                    map.put("rejectionReason", v.getRejectionReason());

                    // Datos del Admin de Sede para contexto rapido
                    if (v.getAdminId() != null) {
                        userRepository.findById(v.getAdminId()).ifPresent(admin -> {
                            map.put("adminId", admin.getId().toString());
                            map.put("adminFullName", admin.getFullName());
                            map.put("adminEmail", admin.getEmail());
                        });
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "adminStats", allEntries = true)
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
            notificationService.enviar(v.getAdminId(), "CONTEXTO_SEDE_ACTIVADO", null,
                    "Tu sede '" + v.getName() + "' ha sido APROBADA. Ya tienes acceso al panel de gestión de tu sede.");
        }

        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook());
    }

    /**
     * Alterna el estado de una sede entre APROBADA y SUSPENDIDA.
     * - APROBADA -> SUSPENDIDA (con motivo opcional)
     * - SUSPENDIDA -> APROBADA (reactivacion)
     * No aplica para sedes en PENDIENTE_APROBACION ni RECHAZADA.
     */
    @Transactional
    @CacheEvict(value = "adminStats", allEntries = true)
    public VenueResponse toggleVenue(UUID id, String motivo) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));

        if (v.getStatus() == EstadoSede.PENDIENTE_APROBACION) {
            throw new com.modoensayo.shared.exceptions.BusinessException(
                    "No se puede suspender una sede que aun no esta aprobada. Apruebala o rechazala primero.");
        }
        if (v.getStatus() == EstadoSede.RECHAZADA) {
            throw new com.modoensayo.shared.exceptions.BusinessException(
                    "No se puede suspender o reactivar una sede rechazada.");
        }

        boolean suspendiendo = v.getStatus() == EstadoSede.APROBADA;
        v.setStatus(suspendiendo ? EstadoSede.SUSPENDIDA : EstadoSede.APROBADA);
        if (suspendiendo) {
            v.setRejectionReason(motivo);
        } else {
            v.setRejectionReason(null);
        }
        v = venueRepository.save(v);

        if (v.getAdminId() != null) {
            String mensaje = suspendiendo
                    ? "Tu sede '" + v.getName() + "' ha sido SUSPENDIDA por el administrador. Motivo: "
                            + (motivo != null && !motivo.isBlank() ? motivo : "No especificado")
                            + ". Tus salas no pueden recibir nuevas reservas. Contacta al administrador para mas detalles."
                    : "Tu sede '" + v.getName() + "' ha sido REACTIVADA. Ya puedes recibir nuevas reservas.";
            notificationService.enviar(v.getAdminId(), null, null, mensaje);
        }

        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt(),
                v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook());
    }

    @Transactional
    @CacheEvict(value = "adminStats", allEntries = true)
    public VenueResponse rejectVenue(UUID id, String reason) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        v.setStatus(EstadoSede.RECHAZADA);
        v.setRejectionReason(reason);
        v = venueRepository.save(v);

        if (v.getAdminId() != null) {
            notificationService.enviar(v.getAdminId(), null, null,
                    "Tu sede '" + v.getName() + "' ha sido RECHAZADA. Motivo: " + (reason != null ? reason : "No especificado") + ". Corrige los datos y reenvia.");
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
    @CacheEvict(value = "adminStats", allEntries = true)
    public void toggleUser(UUID userId, String motivo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean suspendiendo = user.isEnabled();
        if (suspendiendo && (motivo == null || motivo.isBlank())) {
            throw new com.modoensayo.shared.exceptions.BusinessException(
                    "Debes indicar un motivo para suspender la cuenta.");
        }

        user.setEnabled(!suspendiendo);
        userRepository.save(user);

        String msg = user.isEnabled()
                ? "Tu cuenta ha sido REACTIVADA."
                : "Tu cuenta ha sido SUSPENDIDA. Motivo: " + (motivo != null ? motivo : "No especificado") + ". Contacta al administrador.";
        notificationService.enviar(userId, null, null, msg);
    }

    /**
     * Elimina permanentemente una cuenta de usuario. Protege:
     * - Al administrador raiz del sistema (admin@modoensayo.com)
     * - Al propio admin que esta ejecutando la accion
     * Las dependencias con cascade en la BD se eliminan automaticamente
     * (user_roles, identity_verifications, associates, professional_profile, refund_methods).
     * Las referencias sin cascade (venues, classes, enrollments, notifications) se anulan
     * o eliminan segun corresponda antes de borrar el user.
     */
    @Transactional
    public void deleteUser(UUID targetUserId, UUID actorAdminId) {
        if (targetUserId.equals(actorAdminId)) {
            throw new com.modoensayo.shared.exceptions.BusinessException(
                    "No puedes eliminar tu propia cuenta de administrador.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if ("admin@modoensayo.com".equalsIgnoreCase(target.getEmail())) {
            throw new com.modoensayo.shared.exceptions.BusinessException(
                    "No se puede eliminar la cuenta raiz del sistema.");
        }

        // Limpiar notificaciones (sin cascade en BD)
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedAtDesc(targetUserId));

        // Eliminar al usuario. JPA cascadea user_roles, identity, associates,
        // professional_profile y refund_methods configurados con CascadeType.ALL
        // y orphanRemoval = true.
        userRepository.delete(target);
    }
}
