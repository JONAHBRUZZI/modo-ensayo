package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClassRepository extends JpaRepository<Class, UUID> {
    List<Class> findByStatusOrderByStartTimeAsc(ClassStatus status);
    List<Class> findByTeacherId(UUID teacherId);
    List<Class> findByRoomVenueId(UUID venueId);
}
