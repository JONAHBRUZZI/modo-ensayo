package com.modoensayo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundMethodRequest(
        @NotBlank String method,
        @Size(max = 1000) String details
) {}
