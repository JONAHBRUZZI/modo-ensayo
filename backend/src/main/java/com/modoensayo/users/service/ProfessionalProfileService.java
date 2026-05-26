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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProfessionalProfile profile = profileRepository.findByUser_Id(userId)
                .orElse(ProfessionalProfile.builder().user(user).build());

        if (data.containsKey("description")) profile.setDescription(data.get("description"));
        if (data.containsKey("especialidad")) profile.setEspecialidad(data.get("especialidad"));
        if (data.containsKey("nivelEnsenanza")) profile.setNivelEnsenanza(data.get("nivelEnsenanza"));
        if (data.containsKey("formacion")) profile.setFormacion(data.get("formacion"));
        if (data.containsKey("experienceYears")) {
            try { profile.setExperienceYears(Integer.parseInt(data.get("experienceYears"))); } catch (NumberFormatException e) {}
        }
        if (data.containsKey("instagram")) profile.setInstagram(data.get("instagram"));
        if (data.containsKey("youtube")) profile.setYoutube(data.get("youtube"));
        if (data.containsKey("sitioWeb")) profile.setSitioWeb(data.get("sitioWeb"));
        if (data.containsKey("linkedin")) profile.setLinkedin(data.get("linkedin"));

        return profileRepository.save(profile);
    }
}
