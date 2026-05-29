package com.modoensayo.venues.dto;

public record RoomRequest(
    // Datos básicos
    String name,
    Integer capacity,
    Integer tamanoM2,
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

    // Instrumentos disponibles
    Boolean tienePiano,
    Boolean tieneGuitarra,
    Boolean tieneBateria,

    // Legacy
    String equipment
) {}
