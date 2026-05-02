package com.modoensayo.reschedules.controller;

import com.modoensayo.reschedules.dto.RescheduleRequest;
import com.modoensayo.reschedules.dto.RescheduleResponseDto;
import com.modoensayo.reschedules.dto.StudentDecisionRequest;
import com.modoensayo.reschedules.dto.TeacherDecisionRequest;
import com.modoensayo.reschedules.dto.NotificationDto;
import com.modoensayo.reschedules.service.RescheduleService;
import com.modoensayo.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reschedules")
@RequiredArgsConstructor
public class RescheduleController {

    private final RescheduleService rescheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<RescheduleResponseDto> propose(@Valid @RequestBody RescheduleRequest request) {
        UUID teacherId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(rescheduleService.proposeReschedule(teacherId, request));
    }

    @PostMapping("/teacher-decision")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<RescheduleResponseDto> teacherDecision(@Valid @RequestBody TeacherDecisionRequest request) {
        UUID teacherId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(rescheduleService.teacherDecision(teacherId, request));
    }

    @PostMapping("/student-decision")
    @PreAuthorize("hasAnyRole('USER', 'TEACHER', 'ADMIN')")
    public ResponseEntity<RescheduleResponseDto> studentDecision(@Valid @RequestBody StudentDecisionRequest request) {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(rescheduleService.studentDecision(userId, request));
    }

    @GetMapping("/{rescheduleId}")
    public ResponseEntity<RescheduleResponseDto> getReschedule(@PathVariable UUID rescheduleId) {
        return ResponseEntity.ok(rescheduleService.getReschedule(rescheduleId));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<RescheduleResponseDto>> getByClass(@PathVariable UUID classId) {
        return ResponseEntity.ok(rescheduleService.getReschedulesByClass(classId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(rescheduleService.getNotifications(userId));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Long> getUnreadCount() {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(rescheduleService.getUnreadNotificationCount(userId));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        rescheduleService.markNotificationRead(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead() {
        UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId());
        rescheduleService.markAllNotificationsRead(userId);
        return ResponseEntity.ok().build();
    }
}
