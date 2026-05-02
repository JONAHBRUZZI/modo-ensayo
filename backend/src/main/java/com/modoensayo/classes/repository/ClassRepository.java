package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClassRepository extends JpaRepository<Class, UUID> {
    List<Class> findByStatus(ClassStatus status);
    List<Class> findByStatusAndStartTimeAfter(ClassStatus status, Instant now);
}
