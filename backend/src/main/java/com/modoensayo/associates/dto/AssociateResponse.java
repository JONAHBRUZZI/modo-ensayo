package com.modoensayo.associates.dto;

import java.time.Instant;
import java.util.UUID;

public record AssociateResponse(UUID id, String email, String status, Instant createdAt) {}
