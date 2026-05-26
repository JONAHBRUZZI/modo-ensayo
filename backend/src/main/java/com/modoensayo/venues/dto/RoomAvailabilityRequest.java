package com.modoensayo.venues.dto;

import java.time.Instant;

public record RoomAvailabilityRequest(Instant startTime, Instant endTime) {}
