package com.modoensayo.auth.service;

import com.modoensayo.auth.dto.AuthResponse;
import com.modoensayo.auth.dto.GoogleAuthRequest;
import com.modoensayo.auth.dto.LoginRequest;
import com.modoensayo.auth.dto.RegisterRequest;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ConflictException;
import com.modoensayo.shared.security.JwtUtil;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Este correo ya está registrado. ¿Quieres iniciar sesión o recuperar tu contraseña?");
        }
        if (req.rut() != null && !req.rut().isBlank() && userRepository.existsByRut(req.rut())) {
            throw new ConflictException("Este RUT ya está registrado. Si es tu cuenta, recupera tu contraseña.");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .rut(req.rut())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BusinessException("Default role not found"));
        UserRole ur = UserRole.builder()
                .user(user)
                .role(userRole)
                .build();
        userRoleRepository.save(ur);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new BusinessException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest req) {
        // Verificar el token contra Google
        String email;
        String fullName;
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + req.credential()))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("Token de Google invalido o expirado");
            }
            var json = new ObjectMapper().readTree(response.body());
            email = json.get("email").asText();
            fullName = json.has("name") ? json.get("name").asText() : email.split("@")[0];
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Error al verificar la cuenta de Google");
        }

        // Buscar usuario existente o crear uno nuevo
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .enabled(true)
                    .build();
            user = userRepository.save(user);

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new BusinessException("Default role not found"));
            userRoleRepository.save(UserRole.builder().user(user).role(userRole).build());
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());

        return new AuthResponse(token, refreshToken,
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getFullName(),
                        user.getSocialName(), user.getPhone(), roles, user.isEnabled()));
    }
}
