package com.modoensayo.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(UUID id, UUID classId, UUID beneficiaryId, String beneficiaryType,
                                 Boolean present, Instant createdAt) {}
