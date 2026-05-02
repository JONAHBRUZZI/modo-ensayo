package com.modoensayo.users.dto;

public record UserProfileResponse(
        String id,
        String email,
        String fullName,
        String phone,
        java.util.List<String> roles
) {}
