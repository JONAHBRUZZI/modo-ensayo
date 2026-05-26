package com.modoensayo.classes.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassResponse(
    UUID id, String title, String discipline, String level, String description,
    Integer capacity, Integer duration, Double price, Integer minAge, Integer maxAge,
    Instant startTime, UUID roomId, String roomName, UUID venueId, String venueName,
    UUID teacherId, String status, String tipoClase, Integer enrolledCount, Instant createdAt
) {}
