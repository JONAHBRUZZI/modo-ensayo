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
import java.util.Map;
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
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) Integer edadMin,
            @RequestParam(required = false) Integer edadMax) {

        boolean hasFilters = disciplina != null || comuna != null || fechaDesde != null
                || fechaHasta != null || precioMin != null || precioMax != null
                || nivel != null || edadMin != null || edadMax != null;

        if (hasFilters) {
            return ResponseEntity.ok(classService.search(disciplina, comuna, fechaDesde, fechaHasta,
                    precioMin, precioMax, nivel, edadMin, edadMax));
        }
        return ResponseEntity.ok(classService.listPublished());
    }

    @GetMapping("/disciplines")
    public ResponseEntity<List<Map<String, Object>>> getDisciplines() {
        return ResponseEntity.ok(classService.getAvailableDisciplines());
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
    public ResponseEntity<?> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody ClassRequest req,
                                                 @RequestParam(defaultValue = "false") boolean draft) {
        UUID teacherId = req.teacherId() != null ? req.teacherId() : user.getUserId();
        ClassResponse clase = classService.createWithTeacher(req, teacherId, draft);
        boolean rolAsignado = classService.wasTeacherRoleJustAssigned();
        if (rolAsignado) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("clase", clase);
            response.put("atributosActualizados", true);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(clase);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@AuthenticationPrincipal CustomUserDetails user,
                                             @PathVariable UUID id) {
        classService.deleteDraft(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishClass(@AuthenticationPrincipal CustomUserDetails user,
                                                       @PathVariable UUID id,
                                                       @RequestBody ClassRequest req) {
        ClassResponse clase = classService.completeClass(id, req, user.getUserId());
        boolean rolAsignado = classService.wasTeacherRoleJustAssigned();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("clase", clase);
        if (rolAsignado) response.put("atributosActualizados", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/profesor")
    public ResponseEntity<Map<String, Object>> getProfesor(@PathVariable UUID id) {
        return ResponseEntity.ok(classService.getTeacherProfileForClass(id));
    }
}
