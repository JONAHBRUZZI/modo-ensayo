package com.modoensayo.venues.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RoomAvailabilityRequest(
        @NotNull String roomId,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {}
