package com.modoensayo.associates.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssociateResponse(UUID id, String email, String name, String relationship, LocalDate birthDate, String rut, String status, Instant createdAt) {}
