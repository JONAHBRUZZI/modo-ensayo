package com.modoensayo.reviews.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.EligibleReviewItem;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.enums.ReviewTargetType;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createStudentClassReview(UUID studentId, CreateReviewRequest request) {
        Class classEntity = getCompletedClass(request.classId());
        boolean enrolled = enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
                classEntity.getId(), "USER", studentId);
        if (!enrolled) {
            throw new BusinessException("Solo alumnos inscritos pueden evaluar esta clase");
        }

        ensureNotReviewed(classEntity.getId(), studentId, ReviewTargetType.CLASS, classEntity.getId());

        Review review = saveReview(classEntity.getId(), studentId, ReviewTargetType.CLASS, classEntity.getId(),
                request.score(), request.comment());
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse createTeacherVenueReview(UUID teacherId, CreateReviewRequest request) {
        Class classEntity = getCompletedClass(request.classId());
        if (!teacherId.equals(classEntity.getTeacherId())) {
            throw new BusinessException("Solo el profesor de la clase puede evaluar la sede");
        }

        UUID venueId = classEntity.getRoom().getVenue().getId();
        ensureNotReviewed(classEntity.getId(), teacherId, ReviewTargetType.VENUE, venueId);

        Review review = saveReview(classEntity.getId(), teacherId, ReviewTargetType.VENUE, venueId,
                request.score(), request.comment());
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse createTeacherStudentReview(UUID teacherId, CreateReviewRequest request) {
        if (request.targetId() == null) {
            throw new BusinessException("targetId es obligatorio para evaluar alumno");
        }

        Class classEntity = getCompletedClass(request.classId());
        if (!teacherId.equals(classEntity.getTeacherId())) {
            throw new BusinessException("Solo el profesor de la clase puede evaluar alumnos");
        }

        boolean enrolled = enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
                classEntity.getId(), "USER", request.targetId());
        if (!enrolled) {
            throw new BusinessException("El alumno no pertenece a esta clase");
        }

        ensureNotReviewed(classEntity.getId(), teacherId, ReviewTargetType.STUDENT, request.targetId());

        Review review = saveReview(classEntity.getId(), teacherId, ReviewTargetType.STUDENT, request.targetId(),
                request.score(), request.comment());
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<EligibleReviewItem> getStudentEligibleReviews(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByBeneficiaryId(studentId);
        List<EligibleReviewItem> items = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Class classEntity = classRepository.findById(enrollment.getClassId()).orElse(null);
            if (classEntity == null || !isCompleted(classEntity)) {
                continue;
            }
            boolean already = reviewRepository.existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(
                    classEntity.getId(), studentId, ReviewTargetType.CLASS, classEntity.getId());
            if (already) {
                continue;
            }
            items.add(new EligibleReviewItem(
                    classEntity.getId(),
                    classEntity.getTitle(),
                    classEntity.getEndTime(),
                    ReviewTargetType.CLASS.name(),
                    classEntity.getId(),
                    classEntity.getTitle()
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<EligibleReviewItem> getTeacherEligibleReviews(UUID teacherId) {
        List<Class> classes = classRepository.findByTeacherId(teacherId);
        List<EligibleReviewItem> items = new ArrayList<>();
        for (Class classEntity : classes) {
            if (!isCompleted(classEntity)) {
                continue;
            }

            UUID venueId = classEntity.getRoom().getVenue().getId();
            boolean venueReviewed = reviewRepository.existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(
                    classEntity.getId(), teacherId, ReviewTargetType.VENUE, venueId);
            if (!venueReviewed) {
                items.add(new EligibleReviewItem(
                        classEntity.getId(), classEntity.getTitle(), classEntity.getEndTime(),
                        ReviewTargetType.VENUE.name(), venueId, classEntity.getRoom().getVenue().getName()
                ));
            }

            List<Enrollment> enrollments = enrollmentRepository.findByClassId(classEntity.getId());
            for (Enrollment enrollment : enrollments) {
                UUID studentId = enrollment.getBeneficiaryId();
                if (studentId == null) {
                    continue;
                }
                boolean studentReviewed = reviewRepository.existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(
                        classEntity.getId(), teacherId, ReviewTargetType.STUDENT, studentId);
                if (studentReviewed) {
                    continue;
                }
                User student = userRepository.findById(studentId).orElse(null);
                items.add(new EligibleReviewItem(
                        classEntity.getId(), classEntity.getTitle(), classEntity.getEndTime(),
                        ReviewTargetType.STUDENT.name(), studentId,
                        student != null ? student.getFullName() : studentId.toString()
                ));
            }
        }
        return items;
    }

    private Review saveReview(UUID classId, UUID reviewerId, ReviewTargetType targetType, UUID targetId, Integer score, String comment) {
        Review review = Review.builder()
                .classId(classId)
                .reviewerId(reviewerId)
                .targetType(targetType)
                .targetId(targetId)
                .score(score)
                .comment(comment)
                .createdAt(Instant.now())
                .build();
        return reviewRepository.save(review);
    }

    private void ensureNotReviewed(UUID classId, UUID reviewerId, ReviewTargetType targetType, UUID targetId) {
        boolean exists = reviewRepository.existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(
                classId, reviewerId, targetType, targetId);
        if (exists) {
            throw new BusinessException("Ya existe una evaluacion para este objetivo");
        }
    }

    private Class getCompletedClass(UUID classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!isCompleted(classEntity)) {
            throw new BusinessException("No se puede evaluar una clase no realizada");
        }
        return classEntity;
    }

    private boolean isCompleted(Class classEntity) {
        return classEntity.getStatus() == ClassStatus.COMPLETED
                || (classEntity.getEndTime() != null && classEntity.getEndTime().isBefore(Instant.now()));
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getClassId(),
                review.getReviewerId(),
                review.getTargetType().name(),
                review.getTargetId(),
                review.getScore(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
