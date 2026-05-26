package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomAvailabilityResponse(UUID id, UUID roomId, Instant startTime, Instant endTime) {}
