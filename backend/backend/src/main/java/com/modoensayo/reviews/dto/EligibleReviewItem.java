package com.modoensayo.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record EligibleReviewItem(
        UUID classId,
        String classTitle,
        Instant classEndTime,
        String targetType,
        UUID targetId,
        String targetLabel
) {
}
