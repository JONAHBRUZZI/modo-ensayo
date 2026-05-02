package com.modoensayo.reschedules.dto;

import java.util.UUID;

public record NotificationDto(
        UUID id,
        String message,
        boolean read,
        java.time.Instant createdAt
) {}
