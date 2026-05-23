package com.modoensayo.users.dto;

public record RefundMethodRequest(
    String bank, String accountType, String accountNumber, String accountHolder, String rut
) {}
