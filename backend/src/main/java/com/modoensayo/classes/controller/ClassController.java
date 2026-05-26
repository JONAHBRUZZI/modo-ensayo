package com.modoensayo.classes.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<List<ClassResponse>> listPublished() {
        return ResponseEntity.ok(classService.listPublished());
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody ClassRequest req) {
        return ResponseEntity.ok(classService.createWithTeacher(req, user.getUserId()));
    }
}
