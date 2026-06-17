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

    public ReviewResponse create(UUID reviewerId, CreateReviewRequest req) {
        validarPermiso(reviewerId, req);
        Review r = Review.builder()
                .classId(req.classId()).reviewerId(reviewerId)
                .targetType(req.targetType()).targetId(req.targetId())
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
            default -> "—";
        };
    }
}
