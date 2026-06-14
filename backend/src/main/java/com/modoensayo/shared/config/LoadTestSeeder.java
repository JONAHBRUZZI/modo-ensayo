package com.modoensayo.shared.config;

import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class LoadTestSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        long existing = userRepository.count();
        if (existing >= 50) {
            log.info("LoadTestSeeder: ya hay {} usuarios, omitiendo seed", existing);
            return;
        }

        String hash = passwordEncoder.encode("test123");
        Role userRole = roleRepository.findByName("USER").orElse(null);
        Role teacherRole = roleRepository.findByName("TEACHER").orElse(null);

        int created = 0;
        for (int i = 1; i <= 50; i++) {
            String email = "testuser" + i + "@loadtest.com";
            if (userRepository.existsByEmail(email)) continue;

            User user = User.builder()
                    .email(email)
                    .passwordHash(hash)
                    .fullName("Test User " + i)
                    .enabled(true)
                    .identidadValidada(true)
                    .identidadEstado("APROBADO")
                    .build();
            user = userRepository.save(user);

            if (userRole != null) {
                userRoleRepository.save(UserRole.builder().user(user).role(userRole).build());
            }
            if (i <= 5 && teacherRole != null) {
                userRoleRepository.save(UserRole.builder().user(user).role(teacherRole).build());
            }
            created++;
        }
        log.info("LoadTestSeeder: creados {} usuarios de carga (password: test123)", created);
    }
}
