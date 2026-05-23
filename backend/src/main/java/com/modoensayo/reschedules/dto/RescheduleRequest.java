package com.modoensayo.reschedules.dto;

import java.time.Instant;
import java.util.UUID;

public record RescheduleRequest(UUID classId, Instant proposedTime, String reason) {}
