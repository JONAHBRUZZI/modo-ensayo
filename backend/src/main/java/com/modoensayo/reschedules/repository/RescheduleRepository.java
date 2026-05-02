package com.modoensayo.reschedules.repository;

import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescheduleRepository extends JpaRepository<Reschedule, UUID> {
    List<Reschedule> findByClassId(UUID classId);
    Optional<Reschedule> findByClassIdAndStatus(UUID classId, RescheduleStatus status);
    List<Reschedule> findByStatusAndResponseDeadlineBefore(RescheduleStatus status, Instant deadline);
    List<Reschedule> findByTeacherId(UUID teacherId);
}
