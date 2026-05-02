package com.modoensayo.venues.dto;

public record RoomResponse(
        String id,
        String venueId,
        String name,
        Integer capacity,
        String floorType,
        Boolean hasMirrors,
        Boolean hasSound
) {}
