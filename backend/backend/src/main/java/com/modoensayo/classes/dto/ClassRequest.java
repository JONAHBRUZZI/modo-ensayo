package com.modoensayo.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ClassRequest(
        @NotNull String roomId,
        @NotBlank String title,
        String discipline,
        @NotNull Integer capacity,
        @NotNull Integer price,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {}
