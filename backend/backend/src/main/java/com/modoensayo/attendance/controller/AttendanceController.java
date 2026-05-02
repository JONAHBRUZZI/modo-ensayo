package com.modoensayo.attendance.controller;

import com.modoensayo.attendance.dto.AttendanceRequest;
import com.modoensayo.attendance.dto.AttendanceResponse;
import com.modoensayo.attendance.service.AttendanceService;
import com.modoensayo.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        String teacherId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.markAttendance(teacherId, request));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'VENUE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(@PathVariable UUID classId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByClass(classId));
    }
}
