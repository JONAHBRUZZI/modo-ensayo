package com.modoensayo.associates.dto;

import java.time.LocalDate;

public record AssociateRequest(String email, String name, String relationship, LocalDate birthDate, String rut) {}
