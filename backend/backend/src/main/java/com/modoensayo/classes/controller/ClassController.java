package com.modoensayo.classes.controller;

import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ResponseEntity<List<ClassResponse>> listPublished() {
        return ResponseEntity.ok(classService.listPublished());
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classService.create(userDetails.getUsername(), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClassResponse> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(classService.updateStatus(id, status, userDetails.getUsername()));
    }
}

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
class TeacherClassController {

    private final ClassRepository classRepository;

    @GetMapping("/classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<ClassResponse>> getMyClasses(@AuthenticationPrincipal UserDetails userDetails) {
        UUID teacherId = UUID.fromString(userDetails.getUsername());
        List<ClassResponse> classes = classRepository.findByTeacherId(teacherId).stream()
                .map(c -> new ClassResponse(
                        c.getId().toString(),
                        c.getRoom().getId().toString(),
                        c.getRoom().getVenue().getName(),
                        c.getTeacherId().toString(),
                        c.getTitle(),
                        c.getDiscipline(),
                        c.getCapacity(),
                        c.getPrice(),
                        c.getStartTime(),
                        c.getEndTime(),
                        c.getStatus().name()
                ))
                .toList();
        return ResponseEntity.ok(classes);
    }
}
