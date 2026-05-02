package com.modoensayo.reschedules.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StudentDecisionRequest(
        @NotNull UUID rescheduleId,
        @NotNull boolean accepted
) {}
