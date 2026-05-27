package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(UUID id, UUID venueId, String venueName, String name, Integer capacity,
                           String floorType, String type, String equipment, Double pricePerHour, Instant createdAt) {}
