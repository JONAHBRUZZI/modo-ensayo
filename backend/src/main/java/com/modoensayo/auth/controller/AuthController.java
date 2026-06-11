package com.modoensayo.auth.controller;

import com.modoensayo.auth.dto.AuthResponse;
import com.modoensayo.auth.dto.GoogleAuthRequest;
import com.modoensayo.auth.dto.LoginRequest;
import com.modoensayo.auth.dto.RegisterRequest;
import com.modoensayo.auth.service.AuthService;
import com.modoensayo.auth.service.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.googleAuth(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklistService.invalidate(authHeader.substring(7));
        }
        return ResponseEntity.ok().build();
    }
}
