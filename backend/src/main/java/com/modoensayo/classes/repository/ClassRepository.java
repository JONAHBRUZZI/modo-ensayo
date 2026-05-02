package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClassRepository extends JpaRepository<Class, UUID> {
    List<Class> findByStatus(ClassStatus status);
    List<Class> findByStatusAndStartTimeAfter(ClassStatus status, Instant now);
    List<Class> findByStatusAndEndTimeBefore(ClassStatus status, Instant now);
    List<Class> findByTeacherId(UUID teacherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Class> findWithLockById(UUID id);

    List<Class> findByStatusAndRoomVenueId(ClassStatus status, UUID venueId);
    List<Class> findByStatusInAndRoomVenueId(List<ClassStatus> statuses, UUID venueId);
}
