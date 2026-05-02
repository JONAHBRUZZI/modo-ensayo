package com.modoensayo.reviews.controller;

import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.EligibleReviewItem;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.service.ReviewService;
import com.modoensayo.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/student/class")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> createStudentClassReview(@Valid @RequestBody CreateReviewRequest request) {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createStudentClassReview(userId, request));
    }

    @PostMapping("/teacher/venue")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> createTeacherVenueReview(@Valid @RequestBody CreateReviewRequest request) {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createTeacherVenueReview(userId, request));
    }

    @PostMapping("/teacher/student")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> createTeacherStudentReview(@Valid @RequestBody CreateReviewRequest request) {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createTeacherStudentReview(userId, request));
    }

    @GetMapping("/eligible/student")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<EligibleReviewItem>> getStudentEligibleReviews() {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(reviewService.getStudentEligibleReviews(userId));
    }

    @GetMapping("/eligible/teacher")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<EligibleReviewItem>> getTeacherEligibleReviews() {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(reviewService.getTeacherEligibleReviews(userId));
    }
}
