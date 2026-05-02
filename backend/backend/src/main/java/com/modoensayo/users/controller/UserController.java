package com.modoensayo.users.controller;

import com.modoensayo.users.domain.RefundMethod;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.dto.RefundMethodRequest;
import com.modoensayo.users.dto.UpdateProfileRequest;
import com.modoensayo.users.dto.UserProfileResponse;
import com.modoensayo.users.service.IdentityVerificationService;
import com.modoensayo.users.service.RefundMethodService;
import com.modoensayo.users.service.UserService;
import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefundMethodService refundMethodService;
    private final IdentityVerificationService identityVerificationService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    @GetMapping("/me/refund-methods")
    public ResponseEntity<List<RefundMethod>> getRefundMethods() {
        return ResponseEntity.ok(refundMethodService.findByUser(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/me/refund-methods")
    public ResponseEntity<RefundMethod> createRefundMethod(@Valid @RequestBody RefundMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundMethodService.create(SecurityUtils.getCurrentUserId(), request));
    }

    @DeleteMapping("/me/refund-methods/{id}")
    public ResponseEntity<Void> deleteRefundMethod(@PathVariable String id) {
        refundMethodService.delete(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> getIdentityVerification() {
        return ResponseEntity.ok(identityVerificationService.findByUser(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/me/identity-verification")
    public ResponseEntity<IdentityVerificationResponse> uploadIdentity(@RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identityVerificationService.upload(SecurityUtils.getCurrentUserId(), request.get("documentUrl")));
    }
}
