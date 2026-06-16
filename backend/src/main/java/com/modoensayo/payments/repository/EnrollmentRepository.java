package com.modoensayo.payments.repository;

import com.modoensayo.payments.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByBeneficiaryId(UUID beneficiaryId);
    List<Enrollment> findByClassId(UUID classId);
    long countByClassId(UUID classId);

    @Query("SELECT e.classId, COUNT(e) FROM Enrollment e WHERE e.classId IN :classIds GROUP BY e.classId")
    List<Object[]> countByClassIdIn(@Param("classIds") List<UUID> classIds);
    boolean existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
            UUID classId, String beneficiaryType, UUID beneficiaryId);
    boolean existsByClassIdAndBeneficiaryId(UUID classId, UUID beneficiaryId);
}
