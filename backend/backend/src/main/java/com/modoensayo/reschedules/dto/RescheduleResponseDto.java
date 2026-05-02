package com.modoensayo.reschedules.dto;

import com.modoensayo.reschedules.enums.RescheduleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RescheduleResponseDto(
        UUID id,
        UUID classId,
        UUID newClassId,
        String classTitle,
        UUID teacherId,
        Instant proposedTime,
        String reason,
        RescheduleStatus status,
        Instant responseDeadline,
        Instant createdAt,
        List<StudentResponseDto> responses
) {}
