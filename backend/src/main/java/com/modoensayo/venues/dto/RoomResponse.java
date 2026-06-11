package com.modoensayo.venues.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    UUID venueId,
    String venueName,
    String name,
    Integer capacity,
    Integer tamanoM2,
    String tipoPiso,
    String floorType,
    String type,
    Double pricePerHour,
    Boolean activa,

    // Equipamiento — espacio
    Boolean hasMirrors,
    Boolean tieneBarraBallet,
    Boolean tieneAireAcondicionado,
    Boolean tieneCalefaccion,
    Boolean tieneInsonorizacion,

    // Equipamiento — audio/video
    Boolean hasSound,
    Boolean tieneAmplificacion,
    Boolean tieneEntradaAuxiliar,
    Boolean tieneMicrofono,
    Boolean tieneEquipoGrabacion,

    // Instrumentos
    Boolean tienePiano,
    Boolean tieneGuitarra,
    Boolean tieneBateria,

    // Legacy
    String equipment,
    String imageUrl,

    Instant createdAt
) {}
