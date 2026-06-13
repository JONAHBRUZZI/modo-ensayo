package com.modoensayo.payments.repository;

import com.modoensayo.payments.domain.PaymentSession;
import com.modoensayo.payments.enums.PaymentSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSessionRepository extends JpaRepository<PaymentSession, UUID> {
    Optional<PaymentSession> findByExternalReference(String externalReference);
    Optional<PaymentSession> findByPreferenceId(String preferenceId);
    long countByStatus(PaymentSessionStatus status);
}
