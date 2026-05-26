package com.modoensayo.users.dto;

import java.time.Instant;
import java.time.LocalDate;

public record IdentityVerificationResponse(
    String id, String userId, String documentUrl, String status,
    String reviewedBy, Instant createdAt,
    String documentType, String documentNumber, String fullName, LocalDate birthDate
) {
    public IdentityVerificationResponse(String id, String userId, String documentUrl, String status, String reviewedBy, Instant createdAt) {
        this(id, userId, documentUrl, status, reviewedBy, createdAt, null, null, null, null);
    }
}
