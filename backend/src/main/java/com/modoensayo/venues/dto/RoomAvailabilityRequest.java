package com.modoensayo.venues.dto;

import java.time.Instant;

import java.util.UUID;

public record RoomAvailabilityRequest(UUID roomId, Instant startTime, Instant endTime) {}
