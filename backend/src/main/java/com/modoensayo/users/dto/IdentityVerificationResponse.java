package com.modoensayo.users.dto;

import java.time.Instant;

public record IdentityVerificationResponse(
    String id, String userId, String documentUrl, String status, String reviewedBy, Instant createdAt
) {}
