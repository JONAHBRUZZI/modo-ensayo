package com.modoensayo.shared.config;

import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotFound("USER");
        createRoleIfNotFound("TEACHER");
        createRoleIfNotFound("VENUE_ADMIN");
        createRoleIfNotFound("ADMIN");

        // Auto-validar identidad de administradores del sistema
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getUserRoles().stream()
                        .anyMatch(ur -> "ADMIN".equals(ur.getRole().getName())))
                .toList();
        for (User admin : admins) {
            IdentityVerification iv = identityVerificationRepository.findByUserId(admin.getId()).orElse(null);
            if (iv == null) {
                iv = IdentityVerification.builder()
                        .userId(admin.getId())
                        .status("APPROVED")
                        .build();
                identityVerificationRepository.save(iv);
            } else if (!"APPROVED".equals(iv.getStatus())) {
                iv.setStatus("APPROVED");
                identityVerificationRepository.save(iv);
            }
            if (!admin.isIdentidadValidada()) {
                admin.setIdentidadValidada(true);
                admin.setIdentidadEstado("APROBADO");
                userRepository.save(admin);
            }
        }
    }

    private void createRoleIfNotFound(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder().name(name).build());
            log.info("Created role: {}", name);
        }
    }
}
