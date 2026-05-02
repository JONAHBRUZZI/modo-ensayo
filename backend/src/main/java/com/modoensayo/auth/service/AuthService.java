package com.modoensayo.auth.service;

import com.modoensayo.auth.domain.User;
import com.modoensayo.auth.dto.AuthResponse;
import com.modoensayo.auth.dto.LoginRequest;
import com.modoensayo.auth.dto.RegisterRequest;
import com.modoensayo.auth.repository.UserRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.security.JwtUtil;
import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .build();

        userRepository.save(user);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BusinessException("Default role not found"));

        UserRole userRoleEntity = new UserRole();
        userRoleEntity.setId(new UserRoleId(user.getId(), userRole.getId()));
        userRoleEntity.setUser(user);
        userRoleEntity.setRole(userRole);
        userRoleRepository.save(userRoleEntity);

        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        String token = jwtUtil.generateToken(user.getEmail(), Map.of("roles", roles));

        return new AuthResponse(token, user.getEmail(), user.getFullName(), roles);
    }
}
