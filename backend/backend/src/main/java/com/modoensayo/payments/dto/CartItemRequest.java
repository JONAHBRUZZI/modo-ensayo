package com.modoensayo.payments.dto;

import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull String classId,
        String beneficiaryType,
        String beneficiaryId
) {}
