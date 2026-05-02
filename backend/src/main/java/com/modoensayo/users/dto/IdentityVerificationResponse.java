package com.modoensayo.users.dto;

public record IdentityVerificationResponse(
        String id,
        String userId,
        String documentUrl,
        String status,
        String reviewedBy
) {}
