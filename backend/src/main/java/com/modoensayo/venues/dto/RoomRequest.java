package com.modoensayo.venues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
        @NotNull String venueId,
        @NotBlank String name,
        @NotNull Integer capacity,
        String floorType,
        Boolean hasMirrors,
        Boolean hasSound,
        Boolean hasBalletBar,
        Boolean hasAirConditioning,
        Boolean hasNaturalLight,
        String lighting,
        String wallColor,
        String imageUrl
) {}
