package com.modoensayo.reviews.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceDemoTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private UUID studentId;
    private UUID classId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        classId = UUID.randomUUID();
    }

    @Test
    void demoSabado_allowsEvaluationAfterCompletedClass() {
        Class completedSaturdayClass = buildClass(classId, ClassStatus.COMPLETED, Instant.now().minus(2, ChronoUnit.HOURS));
        when(classRepository.findById(classId)).thenReturn(Optional.of(completedSaturdayClass));
        when(enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(classId, "USER", studentId))
                .thenReturn(true);
        when(reviewRepository.existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(any(), any(), any(), any()))
                .thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });

        var response = reviewService.createStudentClassReview(
                studentId,
                new CreateReviewRequest(classId, classId, 5, "Excelente clase del sabado")
        );

        assertEquals("CLASS", response.targetType());
        assertEquals(5, response.score());
    }

    @Test
    void blocksEvaluationForNotCompletedClass() {
        Class futureClass = buildClass(classId, ClassStatus.PUBLISHED, Instant.now().plus(4, ChronoUnit.HOURS));
        when(classRepository.findById(classId)).thenReturn(Optional.of(futureClass));

        assertThrows(BusinessException.class, () -> reviewService.createStudentClassReview(
                studentId,
                new CreateReviewRequest(classId, classId, 4, "Intento temprano")
        ));
    }

    private Class buildClass(UUID id, ClassStatus status, Instant endTime) {
        Venue venue = Venue.builder().id(UUID.randomUUID()).name("Sede Centro").build();
        Room room = Room.builder().id(UUID.randomUUID()).venue(venue).build();
        return Class.builder()
                .id(id)
                .teacherId(UUID.randomUUID())
                .room(room)
                .title("Jazz Intermedio")
                .status(status)
                .endTime(endTime)
                .build();
    }
}
