package com.modoensayo.users.dto;

import java.time.Instant;
import java.util.UUID;

public record IdentityVerificationResponse(
    UUID id, UUID userId, String documentUrl, String status, Instant createdAt
) {}
