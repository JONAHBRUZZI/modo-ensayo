package com.modoensayo.users.service;

import com.modoensayo.users.domain.ProfessionalProfile;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.ProfessionalProfileRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfessionalProfile getByUserId(UUID userId) {
        return profileRepository.findByUser_Id(userId).orElse(null);
    }

    /**
     * Verifica si el perfil profesional tiene los datos minimos requeridos para que el
     * maestro sea visible y atractivo para los alumnos.
     * Requeridos: biografia + (disciplinaPrincipal o especialidad).
     */
    public boolean isComplete(UUID userId) {
        ProfessionalProfile p = profileRepository.findByUser_Id(userId).orElse(null);
        if (p == null) return false;
        boolean tieneBiografia = p.getBiografia() != null && !p.getBiografia().isBlank();
        boolean tieneDisciplina = (p.getDisciplinaPrincipal() != null && !p.getDisciplinaPrincipal().isBlank())
                || (p.getEspecialidad() != null && !p.getEspecialidad().isBlank());
        return tieneBiografia && tieneDisciplina;
    }

    @Transactional
    public ProfessionalProfile save(UUID userId, Map<String, String> data) {
        Map<String, Object> objData = new java.util.HashMap<>(data);
        return saveFromObject(userId, objData);
    }

    @Transactional
    public ProfessionalProfile saveFromObject(UUID userId, Map<String, Object> data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProfessionalProfile profile = profileRepository.findByUser_Id(userId)
                .orElse(ProfessionalProfile.builder().user(user).build());

        if (data.containsKey("description")) profile.setDescription(str(data.get("description")));
        if (data.containsKey("especialidad")) profile.setEspecialidad(str(data.get("especialidad")));
        if (data.containsKey("nivelEnsenanza")) profile.setNivelEnsenanza(str(data.get("nivelEnsenanza")));
        if (data.containsKey("formacion")) profile.setFormacion(str(data.get("formacion")));
        if (data.containsKey("experienceYears") && data.get("experienceYears") != null) {
            try {
                profile.setExperienceYears(((Number) data.get("experienceYears")).intValue());
            } catch (ClassCastException ex) {
                try { profile.setExperienceYears(Integer.parseInt(str(data.get("experienceYears")))); } catch (NumberFormatException e) { log.warn("No se pudo parsear experienceYears como entero", e); }
            }
        }
        if (data.containsKey("instagram")) profile.setInstagram(str(data.get("instagram")));
        if (data.containsKey("youtube")) profile.setYoutube(str(data.get("youtube")));
        if (data.containsKey("sitioWeb")) profile.setSitioWeb(str(data.get("sitioWeb")));
        if (data.containsKey("linkedin")) profile.setLinkedin(str(data.get("linkedin")));
        if (data.containsKey("photoUrl")) profile.setPhotoUrl(str(data.get("photoUrl")));

        // Campos nuevos Fix #3
        if (data.containsKey("biografia")) profile.setBiografia(str(data.get("biografia")));
        if (data.containsKey("disciplinaPrincipal")) profile.setDisciplinaPrincipal(str(data.get("disciplinaPrincipal")));
        if (data.containsKey("detalleFormacion")) profile.setDetalleFormacion(str(data.get("detalleFormacion")));
        if (data.containsKey("disciplinasSecundarias")) profile.setDisciplinasSecundarias(toStringList(data.get("disciplinasSecundarias")));
        if (data.containsKey("tipoFormacion")) profile.setTipoFormacion(toStringList(data.get("tipoFormacion")));

        return profileRepository.save(profile);
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        // Soporte para string separado por comas enviado desde formularios simples
        String s = val.toString().trim();
        if (s.isBlank()) return Collections.emptyList();
        return List.of(s.split(",\\s*"));
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }
}
