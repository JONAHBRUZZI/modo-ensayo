package com.modoensayo.reschedules.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.reschedules.dto.*;
import com.modoensayo.reschedules.service.RescheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<RescheduleResponseDto>> getByClass(@PathVariable UUID classId) {
        return ResponseEntity.ok(rescheduleService.getByClass(classId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rescheduleService.getNotifications(user.getUserId()));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rescheduleService.getUnreadCount(user.getUserId()));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        rescheduleService.markRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails user) {
        rescheduleService.markAllRead(user.getUserId());
        return ResponseEntity.ok().build();
    }
}
