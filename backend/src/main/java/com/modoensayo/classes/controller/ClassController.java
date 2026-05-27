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
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<List<ClassResponse>> listPublished(
            @RequestParam(required = false) String disciplina,
            @RequestParam(required = false) String comuna,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String nivel) {

        boolean hasFilters = disciplina != null || comuna != null || fechaDesde != null
                || fechaHasta != null || precioMin != null || precioMax != null || nivel != null;

        if (hasFilters) {
            return ResponseEntity.ok(classService.search(disciplina, comuna, fechaDesde, fechaHasta, precioMin, precioMax, nivel));
        }
        return ResponseEntity.ok(classService.listPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(classService.getById(id));
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<ClassResponse>> getByVenue(@PathVariable UUID venueId) {
        return ResponseEntity.ok(classService.getByVenue(venueId));
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody ClassRequest req,
                                                 @RequestParam(defaultValue = "false") boolean draft) {
        UUID teacherId = req.teacherId() != null ? req.teacherId() : user.getUserId();
        return ResponseEntity.ok(classService.createWithTeacher(req, teacherId, draft));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ClassResponse> publishClass(@AuthenticationPrincipal CustomUserDetails user,
                                                       @PathVariable UUID id,
                                                       @RequestBody ClassRequest req) {
        return ResponseEntity.ok(classService.completeClass(id, req, user.getUserId()));
    }
}
