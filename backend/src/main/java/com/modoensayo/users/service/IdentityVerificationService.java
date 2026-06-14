package com.modoensayo.users.service;

import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.reschedules.service.NotificationService;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final IdentityVerificationRepository identityVerificationRepository;
    private final NotificationService notificationService;

    @Transactional
    public IdentityVerificationResponse upload(String userId, String documentUrl) {
        IdentityVerification existing = identityVerificationRepository.findByUserId(UUID.fromString(userId)).orElse(null);
        if (existing != null) {
            existing.setDocumentUrl(documentUrl);
            existing.setStatus("PENDING");
            return toResponse(identityVerificationRepository.save(existing));
        }

        IdentityVerification verification = IdentityVerification.builder()
                .userId(UUID.fromString(userId))
                .documentUrl(documentUrl)
                .status("PENDING")
                .build();
        return toResponse(identityVerificationRepository.save(verification));
    }

    @Transactional(readOnly = true)
    public IdentityVerificationResponse findByUser(String userId) {
        IdentityVerification verification = identityVerificationRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));
        return toResponse(verification);
    }

    @Transactional(readOnly = true)
    public List<IdentityVerificationResponse> listPending() {
        return identityVerificationRepository.findByStatus("PENDING").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IdentityVerificationResponse review(String verificationId, String action, String adminId) {
        IdentityVerification verification = identityVerificationRepository.findById(UUID.fromString(verificationId))
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found"));

        verification.setStatus(action);
        verification.setReviewedBy(UUID.fromString(adminId));
        IdentityVerification saved = identityVerificationRepository.save(verification);

        String message = "APPROVED".equals(action)
                ? "Tu verificacion de identidad fue aprobada. Ya puedes continuar con tu cuenta."
                : "Tu verificacion de identidad fue rechazada. Sube nuevamente tu documento para reintentar.";

        notificationService.enviar(saved.getUserId(), null, null, message);

        return toResponse(saved);
    }

    private IdentityVerificationResponse toResponse(IdentityVerification v) {
        return new IdentityVerificationResponse(
                v.getId().toString(),
                v.getUserId().toString(),
                v.getDocumentUrl(),
                v.getStatus(),
                v.getReviewedBy() != null ? v.getReviewedBy().toString() : null,
                v.getCreatedAt(),
                v.getDocumentType(),
                v.getDocumentNumber(),
                v.getFullName(),
                v.getBirthDate()
        );
    }
}
