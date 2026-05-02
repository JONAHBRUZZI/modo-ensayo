package com.modoensayo.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReviewRequest(
        @NotNull UUID classId,
        UUID targetId,
        @NotNull @Min(1) @Max(5) Integer score,
        String comment
) {
}
