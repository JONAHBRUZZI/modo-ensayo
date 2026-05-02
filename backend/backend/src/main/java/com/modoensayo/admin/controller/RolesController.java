package com.modoensayo.admin.controller;

import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RolesController {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable String userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        Role role = roleRepository.findByName(body.get("roleName")).orElseThrow();
        UserRole userRole = new UserRole(new UserRoleId(user.getId(), role.getId()), user, role);
        userRoleRepository.save(userRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable String userId, @PathVariable String roleName) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        Role role = roleRepository.findByName(roleName).orElseThrow();
        userRoleRepository.delete(new UserRole(new UserRoleId(user.getId(), role.getId()), user, role));
        return ResponseEntity.noContent().build();
    }
}
