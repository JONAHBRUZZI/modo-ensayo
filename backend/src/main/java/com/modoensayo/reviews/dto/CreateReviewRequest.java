package com.modoensayo.reviews.dto;

import java.util.UUID;

public record CreateReviewRequest(UUID classId, UUID targetId, String targetType, Integer score, String comment) {}
