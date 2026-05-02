package com.modoensayo.reschedules.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RescheduleRequest(
        @NotNull UUID classId,
        @NotNull Instant proposedTime,
        String reason
) {}
