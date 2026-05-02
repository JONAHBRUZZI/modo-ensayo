package com.modoensayo.shared.config;

import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Ensuring demo users have BCrypt passwords...");

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() ->
                roleRepository.save(Role.builder().name("ADMIN").build()));
        Role teacherRole = roleRepository.findByName("TEACHER").orElseGet(() ->
                roleRepository.save(Role.builder().name("TEACHER").build()));
        Role userRole = roleRepository.findByName("USER").orElseGet(() ->
                roleRepository.save(Role.builder().name("USER").build()));
        Role venueAdminRole = roleRepository.findByName("VENUE_ADMIN").orElseGet(() ->
                roleRepository.save(Role.builder().name("VENUE_ADMIN").build()));

        User admin = userRepository.findByEmail("admin@test.com").orElseGet(() ->
                User.builder()
                        .email("admin@test.com")
                        .fullName("Admin Principal")
                        .build());
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        userRepository.save(admin);

        User teacher = userRepository.findByEmail("teacher@test.com").orElseGet(() ->
                User.builder()
                        .email("teacher@test.com")
                        .fullName("Profesor Demo")
                        .build());
        teacher.setPasswordHash(passwordEncoder.encode("teacher123"));
        userRepository.save(teacher);

        User regular = userRepository.findByEmail("user@test.com").orElseGet(() ->
                User.builder()
                        .email("user@test.com")
                        .fullName("Usuario Demo")
                        .build());
        regular.setPasswordHash(passwordEncoder.encode("user123"));
        userRepository.save(regular);

        User venueAdmin = userRepository.findByEmail("venueadmin@test.com").orElseGet(() ->
                User.builder()
                        .email("venueadmin@test.com")
                        .fullName("Director Sede Demo")
                        .build());
        venueAdmin.setPasswordHash(passwordEncoder.encode("venue123"));
        userRepository.save(venueAdmin);

        userRoleRepository.findById(new UserRoleId(admin.getId(), adminRole.getId())).orElseGet(() ->
                userRoleRepository.save(new UserRole(new UserRoleId(admin.getId(), adminRole.getId()), admin, adminRole)));
        userRoleRepository.findById(new UserRoleId(teacher.getId(), teacherRole.getId())).orElseGet(() ->
                userRoleRepository.save(new UserRole(new UserRoleId(teacher.getId(), teacherRole.getId()), teacher, teacherRole)));
        userRoleRepository.findById(new UserRoleId(regular.getId(), userRole.getId())).orElseGet(() ->
                userRoleRepository.save(new UserRole(new UserRoleId(regular.getId(), userRole.getId()), regular, userRole)));
        userRoleRepository.findById(new UserRoleId(venueAdmin.getId(), venueAdminRole.getId())).orElseGet(() ->
                userRoleRepository.save(new UserRole(new UserRoleId(venueAdmin.getId(), venueAdminRole.getId()), venueAdmin, venueAdminRole)));

        log.info("Demo credentials:");
        log.info("  admin@test.com / admin123 (ADMIN)");
        log.info("  teacher@test.com / teacher123 (TEACHER)");
        log.info("  user@test.com / user123 (USER)");
        log.info("  venueadmin@test.com / venue123 (VENUE_ADMIN)");
    }
}
