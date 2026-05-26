package com.modoensayo.users.service;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.*;
import com.modoensayo.users.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final RefundMethodRepository refundMethodRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    @Transactional
    public UserProfileResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest req) {
        User user = getUser(userDetails.getUserId());
        if (req.socialName() != null) user.setSocialName(req.socialName());
        if (req.phone() != null) user.setPhone(req.phone());
        user = userRepository.save(user);
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    public List<RefundMethod> getRefundMethods(CustomUserDetails userDetails) {
        return refundMethodRepository.findByUserId(userDetails.getUserId());
    }

    @Transactional
    public RefundMethod createRefundMethod(CustomUserDetails userDetails, RefundMethodRequest req) {
        RefundMethod rm = RefundMethod.builder()
                .userId(userDetails.getUserId())
                .bank(req.bank())
                .accountType(req.accountType())
                .accountNumber(req.accountNumber())
                .accountHolder(req.accountHolder())
                .rut(req.rut())
                .build();
        return refundMethodRepository.save(rm);
    }

    @Transactional
    public void deleteRefundMethod(UUID userId, UUID id) {
        RefundMethod method = refundMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund method not found"));
        if (!method.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Refund method not found");
        }
        refundMethodRepository.delete(method);
    }

    @Transactional
    public UserProfileResponse setPreferredRefundMethod(CustomUserDetails userDetails, UUID methodId) {
        User user = getUser(userDetails.getUserId());
        user.setPreferredRefundMethodId(methodId);
        user = userRepository.save(user);
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    public IdentityVerificationResponse getIdentityVerification(CustomUserDetails userDetails) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No verification found"));
        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt());
    }

    @Transactional
    public IdentityVerificationResponse uploadIdentity(CustomUserDetails userDetails, String documentUrl) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userDetails.getUserId())
                .orElse(IdentityVerification.builder().userId(userDetails.getUserId()).build());
        iv.setDocumentUrl(documentUrl);
        iv.setStatus("PENDING");
        iv = identityVerificationRepository.save(iv);
        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt());
    }

    @Transactional
    public void deleteIdentityDocument(UUID userId) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No verification found"));
        iv.setDocumentUrl(null);
        identityVerificationRepository.save(iv);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("La nueva contrasena debe tener al menos 6 caracteres");
        }
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("La contrasena actual es incorrecta");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse mapToProfile(User user, IdentityVerification iv) {
        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName()).collect(Collectors.toSet());
        boolean identidadValidada = iv != null && "APPROVED".equals(iv.getStatus());
        boolean identidadEnRevision = iv != null && "PENDING".equals(iv.getStatus());
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getSocialName(), user.getPhone(), user.getRut(), roles, user.isEnabled(),
                identidadValidada, identidadEnRevision, user.getPreferredRefundMethodId());
    }
}
