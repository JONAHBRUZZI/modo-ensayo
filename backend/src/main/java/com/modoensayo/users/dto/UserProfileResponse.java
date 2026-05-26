package com.modoensayo.users.dto;

import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
    UUID id, String email, String fullName, String socialName, String phone,
    String rut, Set<String> roles, boolean enabled,
    boolean identidadValidada, boolean identidadEnRevision
) {}
