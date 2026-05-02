package com.modoensayo.reschedules.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TeacherDecisionRequest(
        @NotNull UUID rescheduleId,
        @NotNull boolean accepted
) {}
