package com.modoensayo.classes.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.service.ClassService;
import com.modoensayo.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherClassController {

    private final ClassService classService;
    private final PaymentService paymentService;

    @GetMapping("/classes")
    public ResponseEntity<List<ClassResponse>> getMyClasses(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(classService.getTeacherClasses(user.getUserId()));
    }

    @GetMapping("/classes/propias")
    public ResponseEntity<List<ClassResponse>> getPropias(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(classService.getTeacherPropias(user.getUserId()));
    }

    @GetMapping("/classes/asignadas")
    public ResponseEntity<List<ClassResponse>> getAsignadas(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(classService.getTeacherAsignadas(user.getUserId()));
    }

    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getEarnings(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.getTeacherEarnings(user.getUserId()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(classService.getTeacherMetrics(user.getUserId()));
    }
}
