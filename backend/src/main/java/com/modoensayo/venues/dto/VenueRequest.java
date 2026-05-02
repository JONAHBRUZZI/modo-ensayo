package com.modoensayo.venues.dto;

import jakarta.validation.constraints.NotBlank;

public record VenueRequest(
        @NotBlank String name,
        String address,
        String description,
        String imageUrl,
        String phone,
        String email
) {}
