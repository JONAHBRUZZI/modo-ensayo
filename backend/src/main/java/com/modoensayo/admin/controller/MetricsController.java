package com.modoensayo.admin.controller;

import com.modoensayo.admin.service.MetricsService;
import com.modoensayo.auth.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/teacher/metrics")
    public ResponseEntity<Map<String, Object>> teacherMetrics(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(metricsService.getTeacherMetrics(user.getUserId()));
    }

    @GetMapping("/teacher/earnings")
    public ResponseEntity<List<Map<String, Object>>> teacherEarnings(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(metricsService.getTeacherEarnings(user.getUserId()));
    }

    @GetMapping("/venue-admin/metrics")
    public ResponseEntity<Map<String, Object>> venueMetrics(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(metricsService.getVenueMetrics(user.getUserId()));
    }

    @GetMapping("/venue-admin/professors")
    public ResponseEntity<List<Map<String, Object>>> venueProfessors(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(metricsService.getVenueProfessors(user.getUserId()));
    }
}
