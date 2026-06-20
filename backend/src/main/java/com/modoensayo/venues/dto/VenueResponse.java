package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

public record VenueResponse(
    UUID id,
    String name,
    String city,
    String region,
    String comuna,
    String address,
    String description,
    String phone,
    String email,
    String status,
    String tipo,
    Instant createdAt,
    // Redes sociales
    String instagram,
    String youtube,
    String sitioWeb,
    String facebook
) {}
