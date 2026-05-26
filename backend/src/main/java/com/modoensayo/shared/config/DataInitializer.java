package com.modoensayo.shared.config;

import com.modoensayo.users.domain.Role;
import com.modoensayo.users.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotFound("USER");
        createRoleIfNotFound("TEACHER");
        createRoleIfNotFound("VENUE_ADMIN");
        createRoleIfNotFound("ADMIN");
    }

    private void createRoleIfNotFound(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder().name(name).build());
            log.info("Created role: {}", name);
        }
    }
}
