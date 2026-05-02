package com.modoensayo.payments.domain;

import com.modoensayo.payments.enums.PaymentSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_sessions")
public class PaymentSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, unique = true)
    private String externalReference;

    @Column(unique = true)
    private String preferenceId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String cartSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSessionStatus status;

    private String mercadoPagoPaymentId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;
}
