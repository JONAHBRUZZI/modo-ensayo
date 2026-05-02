package com.modoensayo.payments.dto;

public record MercadoPagoPreferenceResponse(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint
) {}
