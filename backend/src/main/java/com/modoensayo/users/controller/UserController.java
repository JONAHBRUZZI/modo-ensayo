package com.modoensayo.users.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.users.domain.RefundMethod;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.dto.*;
import com.modoensayo.users.service.UserService;
import com.modoensayo.users.service.ProfessionalProfileService;
import com.modoensayo.users.domain.ProfessionalProfile;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.enums.ClassStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfessionalProfileService profileService;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final UserRepository userRepository;

    /**
     * Retorna si el usuario tiene clases propias activas (futuras/en curso) o clases asignadas activas.
     * El frontend usa esto para decidir si mostrar el botón "Maestro" en el interruptor.
     */
    @GetMapping("/me/actividad-maestro")
    public ResponseEntity<Map<String, Object>> getActividadMaestro(@AuthenticationPrincipal CustomUserDetails user) {
        Instant ahora = Instant.now();
        boolean tieneReservasActivas = classRepository.findByTeacherId(user.getUserId()).stream()
                .anyMatch(c -> c.getTipoClase() == TipoClase.PROPIA
                        && (c.getStatus() == ClassStatus.PUBLISHED || c.getStatus() == ClassStatus.DRAFT)
                        && c.getEndTime() != null && c.getEndTime().isAfter(ahora));
        boolean tieneAsignacionesActivas = classRepository.findByTeacherId(user.getUserId()).stream()
                .anyMatch(c -> c.getTipoClase() == TipoClase.ASIGNADA
                        && (c.getStatus() == ClassStatus.PUBLISHED || c.getStatus() == ClassStatus.DRAFT)
                        && c.getEndTime() != null && c.getEndTime().isAfter(ahora));
        Map<String, Object> result = new HashMap<>();
        result.put("tieneReservasActivas", tieneReservasActivas);
        result.put("tieneAsignacionesActivas", tieneAsignacionesActivas);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal CustomUserDetails user) {
        var enrollments = enrollmentRepository.findByBeneficiaryId(user.getUserId());
        long total = enrollments.size();
        long proximas = enrollments.stream()
                .map(e -> classRepository.findById(e.getClassId()).orElse(null))
                .filter(c -> c != null && c.getStatus() == ClassStatus.PUBLISHED && c.getStartTime() != null && c.getStartTime().isAfter(Instant.now()))
                .count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClases", total);
        stats.put("proximas", proximas);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/me/atributos")
    public ResponseEntity<Map<String, Object>> getAtributos(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> attrs = new HashMap<>();
        UUID userId = user.getUserId();

        // Cargar entidad User una sola vez para todos los campos persistidos
        com.modoensayo.users.domain.User userEntity = userRepository.findById(userId).orElse(null);

        // Usar campos persistidos en User (sin JOIN a identity_verifications)
        attrs.put("identidadValidada", userEntity != null && userEntity.isIdentidadValidada());
        attrs.put("identidadEstado", userEntity != null ? userEntity.getIdentidadEstado() : "SIN_VALIDAR");

        boolean hasRoleTeacher = user.getRoles().contains("TEACHER");
        attrs.put("hasRoleTeacher", hasRoleTeacher);

        // tieneSedeAprobada: persistido en User desde approveVenue(), fallback a rol VENUE_ADMIN
        boolean tieneSedeAprobada = (userEntity != null && userEntity.isTieneSedeAprobada())
                || user.getRoles().contains("VENUE_ADMIN");
        attrs.put("tieneSedeAprobada", tieneSedeAprobada);

        Instant ahora = Instant.now();
        boolean tieneReservasActivas = classRepository.findByTeacherId(userId).stream()
                .anyMatch(c -> c.getTipoClase() == TipoClase.PROPIA
                        && c.getStatus() != ClassStatus.DRAFT
                        && c.getEndTime() != null && c.getEndTime().isAfter(ahora));
        boolean tieneAsignacionesActivas = classRepository.findByTeacherId(userId).stream()
                .anyMatch(c -> c.getTipoClase() == TipoClase.ASIGNADA
                        && c.getStatus() != ClassStatus.DRAFT
                        && c.getEndTime() != null && c.getEndTime().isAfter(ahora));
        // reservasSinClase = DRAFTs con sala asignada: sala reservada pero clase aún no configurada
        long reservasSinClaseCount = classRepository.findByTeacherId(userId).stream()
                .filter(c -> c.getTipoClase() == TipoClase.PROPIA
                        && c.getStatus() == ClassStatus.DRAFT
                        && c.getRoom() != null)
                .count();
        attrs.put("tieneReservasActivas", tieneReservasActivas);
        attrs.put("tieneAsignacionesActivas", tieneAsignacionesActivas);
        attrs.put("reservasSinClase", reservasSinClaseCount > 0);
        attrs.put("reservasSinClaseCount", (int) reservasSinClaseCount);

        // Perfil profesional completo: solo aplica si el usuario es TEACHER
        attrs.put("perfilProfesionalCompleto", profileService.isComplete(userId));

        return ResponseEntity.ok(attrs);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                                                              @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(user, req));
    }

    @GetMapping("/me/refund-methods")
    public ResponseEntity<List<RefundMethod>> getRefundMethods(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getRefundMethods(user));
    }

    @PostMapping("/me/refund-methods")
    public ResponseEntity<RefundMethod> createRefundMethod(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestBody RefundMethodRequest req) {
        return ResponseEntity.ok(userService.createRefundMethod(user, req));
    }

    @DeleteMapping("/me/refund-methods/{id}")
    public ResponseEntity<Void> deleteRefundMethod(@AuthenticationPrincipal CustomUserDetails user,
                                                    @PathVariable UUID id) {
        userService.deleteRefundMethod(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/preferred-refund-method")
    public ResponseEntity<UserProfileResponse> setPreferredRefundMethod(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> body) {
        UUID methodId = body.get("methodId") != null ? UUID.fromString(body.get("methodId")) : null;
        return ResponseEntity.ok(userService.setPreferredRefundMethod(user, methodId));
    }

    @GetMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> getIdentity(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getIdentityVerification(user));
    }

    @PostMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> uploadIdentity(@AuthenticationPrincipal CustomUserDetails user,
                                                                        @RequestBody Map<String, String> body) {
        java.time.LocalDate birthDate = null;
        if (body.get("birthDate") != null && !body.get("birthDate").isBlank()) {
            birthDate = java.time.LocalDate.parse(body.get("birthDate"));
        }
        return ResponseEntity.ok(userService.uploadIdentity(user,
                body.get("documentUrl"),
                body.get("documentType"),
                body.get("documentNumber"),
                body.get("fullName"),
                birthDate));
    }

    @DeleteMapping("/me/identity-verification")
    public ResponseEntity<Void> deleteIdentity(@AuthenticationPrincipal CustomUserDetails user) {
        userService.deleteIdentityDocument(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/professional-profile")
    public ResponseEntity<ProfessionalProfile> getProfessionalProfile(@AuthenticationPrincipal CustomUserDetails user) {
        ProfessionalProfile profile = profileService.getByUserId(user.getUserId());
        return profile != null ? ResponseEntity.ok(profile) : ResponseEntity.noContent().build();
    }

    @PutMapping("/me/professional-profile")
    public ResponseEntity<ProfessionalProfile> saveProfessionalProfile(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(profileService.save(user.getUserId(), body));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                                @RequestBody Map<String, String> body) {
        userService.changePassword(user.getUserId(), body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok().build();
    }
}
