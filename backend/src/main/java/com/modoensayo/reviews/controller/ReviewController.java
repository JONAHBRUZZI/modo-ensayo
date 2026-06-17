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

    @GetMapping("/mine")
    public ResponseEntity<List<ReviewResponse>> getMine(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getMine(user.getUserId()));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ReviewResponse>> getRecentFromOthers(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getRecentFromOthers(user.getUserId()));
    }

    @GetMapping("/about-me")
    public ResponseEntity<List<ReviewResponse>> getAboutMe(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getAboutMe(user.getUserId()));
    }

    /** Objetivos que el usuario puede valorar ahora (maestros, sedes, alumnos segun su relacion). */
    @GetMapping("/eligible/targets")
    public ResponseEntity<List<EligibleReviewItem>> getEligibleTargets(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.getEligibleTargets(user.getUserId()));
    }

    /** Mi valoracion del sistema Modo Ensayo (null si aun no la dejo). */
    @GetMapping("/system/mine")
    public ResponseEntity<ReviewResponse> miValoracionSistema(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(reviewService.miValoracionSistema(user.getUserId()));
    }

    /** Todas las valoraciones del sistema. Solo Admin General. */
    @GetMapping("/system")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getSystemReviews() {
        return ResponseEntity.ok(reviewService.getSystemReviews());
    }

    /** Analitica de las valoraciones del sistema. Solo Admin General. */
    @GetMapping("/system/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getSystemStats() {
        return ResponseEntity.ok(reviewService.getSystemStats());
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
