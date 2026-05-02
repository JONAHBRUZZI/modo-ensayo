package com.modoensayo.auth.service;

import com.modoensayo.auth.dto.AuthResponse;
import com.modoensayo.auth.dto.LoginRequest;
import com.modoensayo.auth.dto.RegisterRequest;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.security.JwtUtil;
import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed_password")
                .fullName("Test User")
                .build();

        userRole = Role.builder()
                .id(1)
                .name("USER")
                .build();
    }

    @Test
    void register_shouldSucceed_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("new@test.com", "password123", "New User", null);

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(null);
        when(userRoleRepository.findByUserId(any(UUID.class))).thenReturn(List.of(
                new UserRole(new UserRoleId(user.getId(), userRole.getId()), user, userRole)
        ));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Test User", response.fullName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("taken@test.com", "password123", "Test", null);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldSucceed_withValidCredentials() {
        LoginRequest request = new LoginRequest("test@test.com", "password123");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of(
                new UserRole(new UserRoleId(user.getId(), userRole.getId()), user, userRole)
        ));
        when(jwtUtil.generateToken(eq("test@test.com"), anyMap())).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Test User", response.fullName());
    }

    @Test
    void login_shouldThrow_withInvalidPassword() {
        LoginRequest request = new LoginRequest("test@test.com", "wrong_password");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@test.com", "password");
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }
}
