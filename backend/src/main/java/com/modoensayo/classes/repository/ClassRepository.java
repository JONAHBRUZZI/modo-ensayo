package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassRepository extends JpaRepository<Class, UUID>, JpaSpecificationExecutor<Class> {
    List<Class> findByStatusOrderByStartTimeAsc(ClassStatus status);
    List<Class> findByTeacherId(UUID teacherId);
    List<Class> findByRoomVenueId(UUID venueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Class c WHERE c.id = :id")
    Optional<Class> findWithLockById(@Param("id") UUID id);

    List<Class> findByStatusAndEndTimeBefore(ClassStatus status, Instant endTime);

    List<Class> findByStatusAndRoomVenueId(ClassStatus status, UUID venueId);
}
