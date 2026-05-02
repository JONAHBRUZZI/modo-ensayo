package com.modoensayo.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AttendanceRequest(
        @NotNull UUID classId,
        @NotBlank String markedBy,
        List<AttendanceItem> attendees
) {
    public record AttendanceItem(
            @NotNull UUID beneficiaryId,
            @NotBlank String beneficiaryType,
            boolean present
    ) {}
}
