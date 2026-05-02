package com.modoensayo.venues.dto;

public record VenueResponse(
        String id,
        String name,
        String address,
        String description,
        String imageUrl,
        String phone,
        String email,
        String status
) {}
