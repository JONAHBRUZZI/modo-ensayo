package com.modoensayo.venues.dto;

import java.util.List;

public record RoomCharacteristicsResponse(
    List<String> danza,
    List<String> musica,
    List<String> generales
) {}
