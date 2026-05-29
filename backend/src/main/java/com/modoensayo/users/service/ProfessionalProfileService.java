package com.modoensayo.users.service;

import com.modoensayo.users.domain.ProfessionalProfile;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.ProfessionalProfileRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfessionalProfile getByUserId(UUID userId) {
        return profileRepository.findByUser_Id(userId).orElse(null);
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
            } catch (ClassCastException e) {
                try { profile.setExperienceYears(Integer.parseInt(str(data.get("experienceYears")))); } catch (NumberFormatException ignored) {}
            }
        }
        if (data.containsKey("instagram")) profile.setInstagram(str(data.get("instagram")));
        if (data.containsKey("youtube")) profile.setYoutube(str(data.get("youtube")));
        if (data.containsKey("sitioWeb")) profile.setSitioWeb(str(data.get("sitioWeb")));
        if (data.containsKey("linkedin")) profile.setLinkedin(str(data.get("linkedin")));
        if (data.containsKey("photoUrl")) profile.setPhotoUrl(str(data.get("photoUrl")));

        return profileRepository.save(profile);
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }
}
