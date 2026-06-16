package com.modoensayo.integration;

import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        if (userRepository.findByEmail("admin@modoensayo.cl").isEmpty()) {
            User admin = User.builder()
                    .email("admin@modoensayo.cl")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .fullName("Admin Test")
                    .phone("+56900000000")
                    .enabled(true)
                    .identidadValidada(true)
                    .build();
            admin = userRepository.save(admin);
            userRoleRepository.save(new UserRole(new UserRoleId(admin.getId(), adminRole.getId()), admin, adminRole));
        }
    }
}
