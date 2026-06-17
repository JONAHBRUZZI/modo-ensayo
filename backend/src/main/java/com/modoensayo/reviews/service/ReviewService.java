package com.modoensayo.reviews.service;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.EligibleReviewItem;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    /** Objetivo fijo para las valoraciones del sistema Modo Ensayo. */
    private static final UUID SYSTEM_TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ReviewResponse create(UUID reviewerId, CreateReviewRequest req) {
        validarPermiso(reviewerId, req);
        UUID targetId = "SYSTEM".equals(req.targetType()) ? SYSTEM_TARGET_ID : req.targetId();
        Review r = Review.builder()
                .classId(req.classId()).reviewerId(reviewerId)
                .targetType(req.targetType()).targetId(targetId)
                .score(req.score()).comment(req.comment())
                .build();
        return toResponse(reviewRepository.save(r));
    }

    /**
     * Matriz etica de reseñas: valida que el reseñador pueda opinar sobre el objetivo.
     * - Nadie puede reseñarse a si mismo (ni a su propia clase/sede).
     * - Una sede no puede reseñar a otra sede.
     */
    private void validarPermiso(UUID reviewerId, CreateReviewRequest req) {
        String type = req.targetType();
        UUID targetId = req.targetId();
        // El sistema Modo Ensayo lo puede valorar cualquiera, una sola vez.
        if ("SYSTEM".equals(type)) {
            boolean yaValoro = reviewRepository.findByReviewerId(reviewerId).stream()
                    .anyMatch(r -> "SYSTEM".equals(r.getTargetType()));
            if (yaValoro) {
                throw new BusinessException("Ya valoraste Modo Ensayo. Gracias por tu opinión.");
            }
            return;
        }
        if (type == null || targetId == null) {
            throw new BusinessException("Falta el objetivo de la reseña.");
        }
        switch (type) {
            case "CLASS" -> classRepository.findById(targetId).ifPresent(c -> {
                if (reviewerId.equals(c.getTeacherId())) {
                    throw new BusinessException("No puedes reseñar tu propia clase.");
                }
            });
            case "TEACHER", "STUDENT" -> {
                if (reviewerId.equals(targetId)) {
                    throw new BusinessException("No puedes reseñarte a ti mismo.");
                }
            }
            case "VENUE" -> {
                Venue v = venueRepository.findById(targetId)
                        .orElseThrow(() -> new BusinessException("La sede no existe."));
                if (reviewerId.equals(v.getAdminId())) {
                    throw new BusinessException("No puedes reseñar tu propia sede o sala.");
                }
                if (!venueRepository.findByAdminId(reviewerId).isEmpty()) {
                    throw new BusinessException("Una sede no puede reseñar a otra sede.");
                }
            }
            default -> throw new BusinessException("Tipo de reseña no valido: " + type);
        }
    }

    public List<ReviewResponse> getByClass(UUID classId) {
        return reviewRepository.findByClassId(classId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** Todas las valoraciones del sistema Modo Ensayo (las ve el Admin General). */
    public List<ReviewResponse> getSystemReviews() {
        return reviewRepository.findByTargetTypeAndTargetId("SYSTEM", SYSTEM_TARGET_ID).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Analitica de las valoraciones del sistema, con valor para decisiones:
     * promedio, % de satisfaccion, distribucion por estrella, participacion
     * y desglose por tipo de usuario (que perfil lo encuentra mas intuitivo).
     */
    public Map<String, Object> getSystemStats() {
        List<Review> reviews = reviewRepository.findByTargetTypeAndTargetId("SYSTEM", SYSTEM_TARGET_ID);
        int total = reviews.size();

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        double promedio = total == 0 ? 0
                : reviews.stream().mapToInt(Review::getScore).average().orElse(0);
        long satisfechos = reviews.stream().filter(r -> r.getScore() != null && r.getScore() >= 4).count();
        long detractores = reviews.stream().filter(r -> r.getScore() != null && r.getScore() <= 2).count();

        // Distribucion 1..5
        Map<Integer, Long> distribucion = new java.util.TreeMap<>();
        for (int i = 1; i <= 5; i++) distribucion.put(i, 0L);
        reviews.forEach(r -> {
            if (r.getScore() != null) distribucion.merge(r.getScore(), 1L, Long::sum);
        });

        // Desglose por perfil del autor
        Map<String, long[]> acc = new java.util.LinkedHashMap<>(); // rol -> [count, suma]
        acc.put("Alumno", new long[]{0, 0});
        acc.put("Maestro", new long[]{0, 0});
        acc.put("Sede", new long[]{0, 0});
        for (Review r : reviews) {
            String rol = rolPrincipal(r.getReviewerId());
            long[] a = acc.get(rol);
            if (a != null) { a[0]++; a[1] += (r.getScore() != null ? r.getScore() : 0); }
        }
        Map<String, Object> porRol = new java.util.LinkedHashMap<>();
        acc.forEach((rol, a) -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("total", a[0]);
            m.put("promedio", a[0] == 0 ? 0 : Math.round((double) a[1] / a[0] * 10.0) / 10.0);
            porRol.put(rol, m);
        });

        long totalUsuarios = userRepository.count();

        out.put("total", total);
        out.put("promedio", Math.round(promedio * 10.0) / 10.0);
        out.put("satisfaccion", total == 0 ? 0 : Math.round((double) satisfechos * 100 / total));
        out.put("detractores", total == 0 ? 0 : Math.round((double) detractores * 100 / total));
        out.put("distribucion", distribucion);
        out.put("porRol", porRol);
        out.put("participacion", totalUsuarios == 0 ? 0 : Math.round((double) total * 100 / totalUsuarios));
        out.put("totalUsuarios", totalUsuarios);
        return out;
    }

    /** Rol principal del usuario para el desglose (Sede > Maestro > Alumno). */
    private String rolPrincipal(UUID userId) {
        if (userId == null) return "Alumno";
        return userRepository.findById(userId).map(u -> {
            java.util.Set<String> roles = u.getUserRoles().stream()
                    .map(ur -> ur.getRole().getName()).collect(Collectors.toSet());
            if (roles.contains("VENUE_ADMIN")) return "Sede";
            if (roles.contains("TEACHER")) return "Maestro";
            return "Alumno";
        }).orElse("Alumno");
    }

    /** La valoracion del sistema que dejo el usuario, si existe (para no pedirla dos veces). */
    public ReviewResponse miValoracionSistema(UUID userId) {
        return reviewRepository.findByReviewerId(userId).stream()
                .filter(r -> "SYSTEM".equals(r.getTargetType()))
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Objetivos que el usuario puede valorar ahora, segun sus relaciones reales y la etica:
     * - Como alumno: maestros y sedes de las clases que tomo (completadas).
     * - Como maestro: las sedes donde dicto y sus alumnos.
     * - Como sede: los maestros y alumnos de las clases de su sede.
     * Excluye objetivos ya reseñados y respeta la matriz etica (sin auto-reseña, sin sede->sede).
     */
    public List<EligibleReviewItem> getEligibleTargets(UUID userId) {
        boolean esAdminSede = !venueRepository.findByAdminId(userId).isEmpty();
        java.util.Set<String> yaReseñados = reviewRepository.findByReviewerId(userId).stream()
                .map(r -> r.getTargetType() + ":" + r.getTargetId())
                .collect(Collectors.toSet());
        java.util.Map<String, EligibleReviewItem> mapa = new java.util.LinkedHashMap<>();

        java.util.function.BiConsumer<String, UUID> addPersona = (tipo, id) -> {
            if (id == null || id.equals(userId)) return;
            String key = tipo + ":" + id;
            if (yaReseñados.contains(key) || mapa.containsKey(key)) return;
            String nombre = userRepository.findById(id)
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Usuario")
                    .orElse(null);
            if (nombre == null) return;
            mapa.put(key, new EligibleReviewItem(null, null, null, tipo, id, nombre));
        };
        java.util.function.Consumer<com.modoensayo.venues.domain.Room> addSede = (room) -> {
            if (esAdminSede || room == null || room.getVenue() == null) return; // una sede no reseña sedes
            Venue v = room.getVenue();
            String key = "VENUE:" + v.getId();
            if (v.getAdminId() != null && v.getAdminId().equals(userId)) return; // no su propia sede
            if (yaReseñados.contains(key) || mapa.containsKey(key)) return;
            mapa.put(key, new EligibleReviewItem(null, null, null, "VENUE", v.getId(), v.getName()));
        };

        // Como alumno
        enrollmentRepository.findByBeneficiaryId(userId).forEach(e ->
            classRepository.findById(e.getClassId())
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .ifPresent(c -> { addPersona.accept("TEACHER", c.getTeacherId()); addSede.accept(c.getRoom()); }));

        // Como maestro
        classRepository.findByTeacherId(userId).stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .forEach(c -> {
                    addSede.accept(c.getRoom());
                    enrollmentRepository.findByClassId(c.getId())
                            .forEach(en -> addPersona.accept("STUDENT", en.getBeneficiaryId()));
                });

        // Como administrador de sede
        venueRepository.findByAdminId(userId).forEach(v ->
            classRepository.findByRoomVenueId(v.getId()).forEach(c -> {
                addPersona.accept("TEACHER", c.getTeacherId());
                enrollmentRepository.findByClassId(c.getId())
                        .forEach(en -> addPersona.accept("STUDENT", en.getBeneficiaryId()));
            }));

        return new java.util.ArrayList<>(mapa.values());
    }

    public List<ReviewResponse> getByUser(UUID userId) {
        return reviewRepository.findByReviewerId(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** Todas las reseñas que el usuario ha escrito (cualquier tipo), de la mas reciente a la mas antigua. */
    public List<ReviewResponse> getMine(UUID userId) {
        return reviewRepository.findByReviewerId(userId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reseñas publicas recientes de OTROS usuarios (clases, maestros y sedes).
     * Excluye reseñas sobre ALUMNOS: no son publicas, solo las ve quien corresponde.
     */
    public List<ReviewResponse> getRecentFromOthers(UUID userId) {
        return reviewRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .filter(r -> r.getReviewerId() != null && !r.getReviewerId().equals(userId))
                .filter(r -> "CLASS".equals(r.getTargetType())
                          || "TEACHER".equals(r.getTargetType())
                          || "VENUE".equals(r.getTargetType()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reseñas que OTROS han hecho SOBRE el usuario: como alumno (STUDENT),
     * como maestro (TEACHER) y sobre sus sedes (VENUE). Es solo lectura.
     */
    public List<ReviewResponse> getAboutMe(UUID userId) {
        List<Review> result = new java.util.ArrayList<>(reviewRepository.findByTargetId(userId));
        venueRepository.findByAdminId(userId).forEach(v ->
                result.addAll(reviewRepository.findByTargetTypeAndTargetId("VENUE", v.getId())));
        return result.stream()
                .distinct()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getByTarget(String type, UUID targetId) {
        return reviewRepository.findByTargetTypeAndTargetId(type, targetId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getByTeacher(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .flatMap(c -> reviewRepository.findByClassId(c.getId()).stream())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EligibleReviewItem> getStudentEligible(UUID studentId) {
        return enrollmentRepository.findByBeneficiaryId(studentId).stream()
                .map(e -> classRepository.findById(e.getClassId()).orElse(null))
                .filter(c -> c != null && c.getStatus() == ClassStatus.COMPLETED)
                .filter(c -> reviewRepository.findByClassId(c.getId()).stream()
                        .noneMatch(r -> r.getReviewerId().equals(studentId)))
                .map(c -> new EligibleReviewItem(c.getId(), c.getTitle(), c.getStartTime(),
                        "CLASS", c.getId(), c.getTitle()))
                .collect(Collectors.toList());
    }

    public List<EligibleReviewItem> getTeacherEligible(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .flatMap(c -> enrollmentRepository.findByClassId(c.getId()).stream()
                        .filter(e -> reviewRepository.findByClassId(c.getId()).stream()
                                .noneMatch(r -> r.getReviewerId().equals(teacherId)
                                        && r.getTargetId().equals(e.getBeneficiaryId())))
                        .map(e -> new EligibleReviewItem(c.getId(), c.getTitle(), c.getStartTime(),
                                "STUDENT", e.getBeneficiaryId(), "Alumno")))
                .distinct()
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review r) {
        String classTitle = r.getClassId() == null ? null
                : classRepository.findById(r.getClassId()).map(c -> c.getTitle()).orElse(null);
        String authorName = r.getReviewerId() == null ? "Usuario"
                : userRepository.findById(r.getReviewerId())
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Usuario")
                    .orElse("Usuario");
        String targetName = resolverTargetName(r);
        return new ReviewResponse(r.getId(), r.getClassId(), classTitle, r.getReviewerId(), authorName,
                r.getTargetType(), r.getTargetId(), targetName, r.getScore(), r.getComment(), r.getCreatedAt());
    }

    /** Nombre legible de lo que se reseña, segun el tipo. */
    private String resolverTargetName(Review r) {
        if (r.getTargetType() == null || r.getTargetId() == null) return "—";
        return switch (r.getTargetType()) {
            case "CLASS" -> classRepository.findById(r.getTargetId()).map(c -> c.getTitle()).orElse("Clase");
            case "TEACHER", "STUDENT" -> userRepository.findById(r.getTargetId())
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Usuario")
                    .orElse("Usuario");
            case "VENUE" -> venueRepository.findById(r.getTargetId()).map(Venue::getName).orElse("Sede");
            case "SYSTEM" -> "Modo Ensayo";
            default -> "—";
        };
    }
}
