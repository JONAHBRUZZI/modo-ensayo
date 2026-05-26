package com.modoensayo.users.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.users.domain.RefundMethod;
import com.modoensayo.users.dto.*;
import com.modoensayo.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                                                              @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(user, req));
    }

    @GetMapping("/me/refund-methods")
    public ResponseEntity<List<RefundMethod>> getRefundMethods(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getRefundMethods(user));
    }

    @PostMapping("/me/refund-methods")
    public ResponseEntity<RefundMethod> createRefundMethod(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestBody RefundMethodRequest req) {
        return ResponseEntity.ok(userService.createRefundMethod(user, req));
    }

    @DeleteMapping("/me/refund-methods/{id}")
    public ResponseEntity<Void> deleteRefundMethod(@PathVariable UUID id) {
        userService.deleteRefundMethod(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/preferred-refund-method")
    public ResponseEntity<UserProfileResponse> setPreferredRefundMethod(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> body) {
        UUID methodId = body.get("methodId") != null ? UUID.fromString(body.get("methodId")) : null;
        return ResponseEntity.ok(userService.setPreferredRefundMethod(user, methodId));
    }

    @GetMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> getIdentity(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getIdentityVerification(user));
    }

    @PostMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> uploadIdentity(@AuthenticationPrincipal CustomUserDetails user,
                                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.uploadIdentity(user, body.get("documentUrl")));
    }
}
