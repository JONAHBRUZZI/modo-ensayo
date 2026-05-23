package com.modoensayo.classes.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherClassController {

    private final ClassService classService;

    @GetMapping("/classes")
    public ResponseEntity<List<ClassResponse>> getMyClasses(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(classService.getTeacherClasses(user.getUserId()));
    }
}
