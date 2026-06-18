package com.modoensayo.reviews.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceDemoTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private VenueRepository venueRepository;

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
    void create_shouldSaveReview_withValidData() {
        Review saved = Review.builder()
                .id(UUID.randomUUID()).classId(classId).reviewerId(studentId)
                .targetType("CLASS").targetId(classId).score(5).comment("Excelente")
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewResponse response = reviewService.create(studentId,
                new CreateReviewRequest(classId, classId, "CLASS", 5, "Excelente clase del sabado"));

        assertEquals("CLASS", response.targetType());
        assertEquals(5, response.score());
    }

    @Test
    void getStudentEligible_shouldReturnCompletedClassesWithoutReview() {
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).classId(classId).beneficiaryId(studentId).build();
        Venue venue = Venue.builder().id(UUID.randomUUID()).name("Sede Centro").build();
        Room room = Room.builder().id(UUID.randomUUID()).venue(venue).build();
        Class completedClass = Class.builder()
                .id(classId).teacherId(UUID.randomUUID()).room(room)
                .title("Jazz Intermedio").status(ClassStatus.COMPLETED)
                .endTime(Instant.now().minus(2, ChronoUnit.HOURS)).build();

        when(enrollmentRepository.findByBeneficiaryId(studentId)).thenReturn(List.of(enrollment));
        when(classRepository.findById(classId)).thenReturn(Optional.of(completedClass));
        when(reviewRepository.findByClassId(classId)).thenReturn(List.of()); // no reviews yet

        var eligible = reviewService.getStudentEligible(studentId);

        assertEquals(1, eligible.size());
        assertEquals(classId, eligible.get(0).classId());
    }

    @Test
    void getStudentEligible_shouldExclude_whenAlreadyReviewed() {
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).classId(classId).beneficiaryId(studentId).build();
        Venue venue = Venue.builder().id(UUID.randomUUID()).name("Sede").build();
        Room room = Room.builder().id(UUID.randomUUID()).venue(venue).build();
        Class completedClass = Class.builder()
                .id(classId).teacherId(UUID.randomUUID()).room(room)
                .title("Ballet").status(ClassStatus.COMPLETED)
                .endTime(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        Review existingReview = Review.builder()
                .id(UUID.randomUUID()).classId(classId).reviewerId(studentId)
                .targetType("CLASS").targetId(classId).score(4).build();

        when(enrollmentRepository.findByBeneficiaryId(studentId)).thenReturn(List.of(enrollment));
        when(classRepository.findById(classId)).thenReturn(Optional.of(completedClass));
        when(reviewRepository.findByClassId(classId)).thenReturn(List.of(existingReview));

        var eligible = reviewService.getStudentEligible(studentId);

        assertEquals(0, eligible.size());
    }
}
