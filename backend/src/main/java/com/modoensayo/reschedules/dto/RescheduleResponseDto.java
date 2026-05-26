package com.modoensayo.reschedules.dto;

import java.time.Instant;
import java.util.UUID;

public record RescheduleResponseDto(UUID id, UUID classId, UUID teacherId, Instant proposedTime,
                                    String reason, String status, Instant createdAt) {}
