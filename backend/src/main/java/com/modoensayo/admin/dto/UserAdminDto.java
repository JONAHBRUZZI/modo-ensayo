package com.modoensayo.admin.dto;

import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;

import java.util.UUID;
import java.util.stream.Collectors;

public record UserAdminDto(
        UUID id,
        String email,
        String fullName,
        String phone,
        boolean active,
        java.util.List<String> roles
) {
    public static UserAdminDto from(User user) {
        return new UserAdminDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.isEnabled(),
                user.getUserRoles() != null
                        ? user.getUserRoles().stream()
                                .map(UserRole::getRole)
                                .map(r -> r.getName())
                                .collect(Collectors.toList())
                        : java.util.List.of()
        );
    }
}
