package com.modoensayo.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
    String token,
    String refreshToken,
    UserDto user
) {
    public record UserDto(
        UUID id,
        String email,
        String fullName,
        String socialName,
        String phone,
        Set<String> roles,
        boolean enabled
    ) {}
}
