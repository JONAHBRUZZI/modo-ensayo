package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.TipoClase;
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

    @Query("SELECT DISTINCT c FROM Class c LEFT JOIN FETCH c.room r LEFT JOIN FETCH r.venue WHERE c.status = :status ORDER BY c.startTime")
    List<Class> findPublishedWithRoomAndVenue(@Param("status") ClassStatus status);

    List<Class> findByTeacherId(UUID teacherId);
    List<Class> findByRoomVenueId(UUID venueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Class c WHERE c.id = :id")
    Optional<Class> findWithLockById(@Param("id") UUID id);

    List<Class> findByStatusAndEndTimeBefore(ClassStatus status, Instant endTime);

    List<Class> findByStatusAndRoomVenueId(ClassStatus status, UUID venueId);

    @Query("SELECT c FROM Class c WHERE c.room.id = :roomId AND c.status != 'CANCELLED' AND c.status != 'SUSPENDED' AND c.startTime < :endTime AND c.endTime > :startTime")
    List<Class> findConflictingClasses(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    List<Class> findByTeacherIdAndTipoClase(UUID teacherId, TipoClase tipoClase);

    @Query("SELECT DISTINCT c.discipline FROM Class c WHERE c.status = 'PUBLISHED' AND c.discipline IS NOT NULL")
    List<String> findDistinctDisciplinesFromPublished();
}
