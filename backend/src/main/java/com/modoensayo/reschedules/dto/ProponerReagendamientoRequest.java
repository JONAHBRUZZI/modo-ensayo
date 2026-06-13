package com.modoensayo.reschedules.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ProponerReagendamientoRequest(
        @NotNull Instant proposedTime,
        String reason
) {}
