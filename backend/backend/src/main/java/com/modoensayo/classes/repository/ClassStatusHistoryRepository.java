package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.ClassStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassStatusHistoryRepository extends JpaRepository<ClassStatusHistory, UUID> {
    List<ClassStatusHistory> findByClassEntityId(UUID classId);
}
