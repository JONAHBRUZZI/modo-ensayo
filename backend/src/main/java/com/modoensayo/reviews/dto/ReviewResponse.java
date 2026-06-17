package com.modoensayo.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(UUID id, UUID classId, String classTitle, UUID reviewerId, String authorName,
                             String targetType, UUID targetId, Integer score, String comment, Instant createdAt) {}
