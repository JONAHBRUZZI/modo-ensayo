package com.modoensayo.associates.dto;

import java.time.LocalDate;

public record AssociateResponse(
        String id,
        String name,
        String relation,
        LocalDate birthDate,
        String rut
) {}
