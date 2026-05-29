package com.modoensayo.venues.dto;

public record VenueRequest(
    String name,
    String city,
    String address,
    String description,
    String phone,
    String email,
    String tipo,
    // Redes sociales y contacto web (siempre editables)
    String instagram,
    String youtube,
    String sitioWeb,
    String facebook
) {}
