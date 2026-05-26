package com.modoensayo.venues.dto;

import java.time.Instant;

public record RoomAvailabilityResponse(String id, String roomId, String roomName, Instant startTime, Instant endTime) {}
