package com.modoensayo.attendance.controller;

import com.modoensayo.attendance.dto.AttendanceRequest;
import com.modoensayo.attendance.dto.AttendanceResponse;
import com.modoensayo.attendance.service.AttendanceService;
import com.modoensayo.auth.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<List<AttendanceResponse>> markAttendance(@RequestBody AttendanceRequest req,
                                                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(attendanceService.markAttendance(req, user.getEmail()));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(@PathVariable UUID classId) {
        return ResponseEntity.ok(attendanceService.getAttendance(classId));
    }
}
