package com.modoensayo.users.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.users.domain.ProfessionalProfile;
import com.modoensayo.users.dto.UserProfileResponse;
import com.modoensayo.users.service.ProfessionalProfileService;
import com.modoensayo.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints dedicados al contexto Maestro.
 * GET /api/profesor/perfil  — perfil profesional + datos básicos del usuario
 * PUT /api/profesor/perfil  — guardar perfil profesional
 */
@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfesorController {

    private final ProfessionalProfileService profileService;
    private final UserService userService;

    @GetMapping("/perfil")
    public ResponseEntity<Map<String, Object>> getPerfil(@AuthenticationPrincipal CustomUserDetails user) {
        UserProfileResponse userProfile = userService.getProfile(user);
        ProfessionalProfile profile = profileService.getByUserId(user.getUserId());

        Map<String, Object> response = new HashMap<>();
        // Datos básicos del usuario
        response.put("fullName", userProfile.fullName());
        response.put("socialName", userProfile.socialName());
        response.put("email", userProfile.email());
        response.put("phone", userProfile.phone());

        // Datos del perfil profesional (puede ser null si no existe aún)
        if (profile != null) {
            response.put("id", profile.getId());
            response.put("description", profile.getDescription());
            response.put("especialidad", profile.getEspecialidad());
            response.put("nivelEnsenanza", profile.getNivelEnsenanza());
            response.put("formacion", profile.getFormacion());
            response.put("experienceYears", profile.getExperienceYears());
            response.put("instagram", profile.getInstagram());
            response.put("youtube", profile.getYoutube());
            response.put("sitioWeb", profile.getSitioWeb());
            response.put("linkedin", profile.getLinkedin());
            response.put("photoUrl", profile.getPhotoUrl());
            response.put("averageRating", profile.getAverageRating());
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    public ResponseEntity<Map<String, Object>> savePerfil(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {

        ProfessionalProfile saved = profileService.saveFromObject(user.getUserId(), body);
        UserProfileResponse userProfile = userService.getProfile(user);

        Map<String, Object> response = new HashMap<>();
        response.put("fullName", userProfile.fullName());
        response.put("socialName", userProfile.socialName());
        response.put("email", userProfile.email());
        response.put("id", saved.getId());
        response.put("description", saved.getDescription());
        response.put("especialidad", saved.getEspecialidad());
        response.put("nivelEnsenanza", saved.getNivelEnsenanza());
        response.put("formacion", saved.getFormacion());
        response.put("experienceYears", saved.getExperienceYears());
        response.put("instagram", saved.getInstagram());
        response.put("youtube", saved.getYoutube());
        response.put("sitioWeb", saved.getSitioWeb());
        response.put("linkedin", saved.getLinkedin());
        response.put("photoUrl", saved.getPhotoUrl());
        response.put("averageRating", saved.getAverageRating());

        return ResponseEntity.ok(response);
    }
}
