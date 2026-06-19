package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

/** Vista plana de un bloque de horario de sala (evita serializar proxies lazy). */
public record RoomScheduleBlockDto(
        UUID id,
        UUID roomId,
        String roomName,
        Instant startTime,
        Instant endTime,
        String status,
        UUID classId
) {}
