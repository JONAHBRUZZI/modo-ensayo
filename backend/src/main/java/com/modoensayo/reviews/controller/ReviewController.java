package com.modoensayo.reviews.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.EligibleReviewItem;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                  @RequestBody CreateReviewRequest req) {
        return ResponseEntity.ok(reviewService.create(user.getUserId(), req));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ReviewResponse>> getByClass(@PathVariable UUID classId) {
        return ResponseEntity.ok(reviewService.getByClass(classId));
    }

    @GetMapping("/eligible/student")
    public ResponseEntity<List<EligibleReviewItem>> getStudentEligible(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getStudentEligible(user.getUserId()));
    }

    @GetMapping("/eligible/teacher")
    public ResponseEntity<List<EligibleReviewItem>> getTeacherEligible(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getTeacherEligible(user.getUserId()));
    }

    @GetMapping("/target/{type}/{targetId}")
    public ResponseEntity<List<ReviewResponse>> getByTarget(@PathVariable String type,
                                                             @PathVariable UUID targetId) {
        return ResponseEntity.ok(reviewService.getByTarget(type, targetId));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<ReviewResponse>> getByTeacher(@PathVariable UUID teacherId) {
        return ResponseEntity.ok(reviewService.getByTeacher(teacherId));
    }
}
