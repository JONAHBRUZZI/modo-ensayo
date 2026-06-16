package com.modoensayo.reviews.service;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.EligibleReviewItem;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.repository.ReviewRepository;
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

    public ReviewResponse create(UUID reviewerId, CreateReviewRequest req) {
        Review r = Review.builder()
                .classId(req.classId()).reviewerId(reviewerId)
                .targetType(req.targetType()).targetId(req.targetId())
                .score(req.score()).comment(req.comment())
                .build();
        return toResponse(reviewRepository.save(r));
    }

    public List<ReviewResponse> getByClass(UUID classId) {
        return reviewRepository.findByClassId(classId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getByUser(UUID userId) {
        return reviewRepository.findByReviewerId(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
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
        return new ReviewResponse(r.getId(), r.getClassId(), r.getReviewerId(), "Usuario",
                r.getTargetType(), r.getTargetId(), r.getScore(), r.getComment(), r.getCreatedAt());
    }
}
