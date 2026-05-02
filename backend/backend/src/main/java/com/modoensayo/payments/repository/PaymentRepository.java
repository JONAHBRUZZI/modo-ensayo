package com.modoensayo.payments.repository;

import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByEnrollmentId(UUID enrollmentId);
    long countByStatus(PaymentStatus status);
}
