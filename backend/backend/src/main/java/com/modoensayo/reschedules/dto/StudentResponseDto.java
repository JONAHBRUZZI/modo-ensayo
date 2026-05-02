package com.modoensayo.reschedules.dto;

import com.modoensayo.reschedules.enums.ResponseType;
import java.time.Instant;
import java.util.UUID;

public record StudentResponseDto(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        ResponseType responseType,
        Instant respondedAt
) {}
