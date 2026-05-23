package com.modoensayo.reschedules.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(UUID id, String title, String message, String type, boolean read, Instant createdAt) {}
