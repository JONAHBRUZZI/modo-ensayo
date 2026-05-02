package com.modoensayo.associates.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AssociateRequest(
        @NotBlank String name,
        String relation,
        LocalDate birthDate,
        String rut
) {}
