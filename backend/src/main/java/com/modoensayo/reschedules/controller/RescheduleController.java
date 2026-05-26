package com.modoensayo.reschedules.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.reschedules.dto.*;
import com.modoensayo.reschedules.service.RescheduleService;
import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reschedules")
@RequiredArgsConstructor
public class RescheduleController {

    private final RescheduleService rescheduleService;

    @PostMapping
    public ResponseEntity<RescheduleResponseDto> propose(@RequestBody RescheduleRequest req) {
        return ResponseEntity.ok(rescheduleService.propose(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RescheduleResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(rescheduleService.getReschedule(id));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<RescheduleResponseDto>> getByClass(@PathVariable UUID classId) {
        return ResponseEntity.ok(rescheduleService.getByClass(classId));
    }

    @GetMapping("/class/{classId}/available-slots")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailableSlots(@PathVariable UUID classId) {
        return ResponseEntity.ok(rescheduleService.getAvailableSlotsForReschedule(classId));
    }

    @PostMapping("/teacher-decision")
    public ResponseEntity<RescheduleResponseDto> teacherDecision(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {
        UUID rescheduleId = UUID.fromString((String) body.get("rescheduleId"));
        boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
        return ResponseEntity.ok(rescheduleService.teacherDecision(rescheduleId, accepted, user.getUserId()));
    }

    @PostMapping("/student-decision")
    public ResponseEntity<Void> studentDecision(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {
        UUID rescheduleId = UUID.fromString((String) body.get("rescheduleId"));
        boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
        Object confirm = body.get("confirmacion");
        if (confirm == null || !Boolean.TRUE.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        rescheduleService.studentDecision(rescheduleId, accepted, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<List<StudentResponseDto>> getResponses(@PathVariable UUID id) {
        return ResponseEntity.ok(rescheduleService.getStudentResponses(id));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rescheduleService.getNotifications(user.getUserId()));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rescheduleService.getUnreadCount(user.getUserId()));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCountLegacy(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rescheduleService.getUnreadCount(user.getUserId()));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        rescheduleService.markRead(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails user) {
        rescheduleService.markAllRead(user.getUserId());
        return ResponseEntity.ok().build();
    }
}
