package com.modoensayo.admin.controller;

import com.modoensayo.admin.dto.AdminStatsResponse;
import com.modoensayo.admin.service.AdminService;
import com.modoensayo.shared.security.SecurityUtils;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.venues.domain.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/identity-verifications")
    public ResponseEntity<List<IdentityVerificationResponse>> listPendingVerifications() {
        return ResponseEntity.ok(adminService.listPendingVerifications());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @PatchMapping("/identity-verifications/{id}")
    public ResponseEntity<IdentityVerificationResponse> reviewIdentity(
            @PathVariable String id, @RequestParam String action) {
        return ResponseEntity.ok(adminService.reviewIdentity(id, action, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/venues/pending")
    public ResponseEntity<List<Venue>> listPendingVenues() {
        return ResponseEntity.ok(adminService.listPendingVenues());
    }

    @PatchMapping("/venues/{id}/approve")
    public ResponseEntity<Venue> approveVenue(@PathVariable String id) {
        return ResponseEntity.ok(adminService.reviewVenue(id, "APPROVED"));
    }

    @PatchMapping("/venues/{id}/reject")
    public ResponseEntity<Venue> rejectVenue(@PathVariable String id) {
        return ResponseEntity.ok(adminService.reviewVenue(id, "REJECTED"));
    }
}
