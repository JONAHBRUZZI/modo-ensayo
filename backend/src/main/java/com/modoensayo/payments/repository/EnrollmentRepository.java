package com.modoensayo.payments.repository;

import com.modoensayo.payments.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByBeneficiaryId(UUID beneficiaryId);
    boolean existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
            UUID classId, String beneficiaryType, UUID beneficiaryId);
}
