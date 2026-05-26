package com.modoensayo.admin.controller;

import com.modoensayo.admin.service.AdminService;
import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.venues.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/identity-verifications")
    public ResponseEntity<List<IdentityVerificationResponse>> getVerifications() {
        return ResponseEntity.ok(adminService.getIdentityVerifications());
    }

    @PatchMapping("/identity-verifications/{id}")
    public ResponseEntity<IdentityVerificationResponse> review(@PathVariable UUID id,
                                                                @RequestParam String action,
                                                                @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(adminService.reviewIdentity(id, action, user.getUserId()));
    }

    @GetMapping("/venues/pending")
    public ResponseEntity<List<VenueResponse>> getPendingVenues() {
        return ResponseEntity.ok(adminService.getPendingVenues());
    }

    @PatchMapping("/venues/{id}/approve")
    public ResponseEntity<VenueResponse> approveVenue(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.approveVenue(id));
    }

    @PatchMapping("/venues/{id}/reject")
    public ResponseEntity<VenueResponse> rejectVenue(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.rejectVenue(id, body.get("motivo")));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @PostMapping("/users/{id}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        adminService.assignRole(id, body.get("roleName"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID id, @PathVariable String roleName) {
        adminService.revokeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<Void> toggleUser(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        adminService.toggleUser(id, body.get("motivo"));
        return ResponseEntity.ok().build();
    }
}
