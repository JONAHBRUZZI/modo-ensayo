package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

public record VenueResponse(UUID id, String name, String city, String address, String description,
                            String phone, String email, String status, Instant createdAt) {}
