package com.modoensayo.classes.dto;

import java.time.Instant;

public record ClassResponse(
        String id,
        String roomId,
        String venueName,
        String teacherId,
        String title,
        String discipline,
        Integer capacity,
        Integer price,
        Instant startTime,
        Instant endTime,
        String status
) {}
