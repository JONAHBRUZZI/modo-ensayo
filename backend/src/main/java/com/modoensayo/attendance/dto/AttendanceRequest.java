package com.modoensayo.attendance.dto;

import java.util.List;
import java.util.UUID;

public record AttendanceRequest(UUID classId, List<AttendanceItem> items) {
    public record AttendanceItem(UUID beneficiaryId, String beneficiaryType, Boolean present) {}
}
