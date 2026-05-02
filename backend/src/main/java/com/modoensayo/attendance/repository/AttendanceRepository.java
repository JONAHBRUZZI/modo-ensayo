package com.modoensayo.attendance.repository;

import com.modoensayo.attendance.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByClassId(UUID classId);
    boolean existsByClassIdAndBeneficiaryId(UUID classId, UUID beneficiaryId);
    long countByClassIdAndPresentTrue(UUID classId);
}
