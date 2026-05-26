package com.modoensayo.classes.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassRequest(
    String title, String discipline, String level, String description,
    Integer capacity, Integer duration, Double price, Integer minAge, Integer maxAge,
    Instant startTime, UUID roomId
) {}
