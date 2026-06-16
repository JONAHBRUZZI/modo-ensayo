package com.modoensayo.classes.repository;

import com.modoensayo.classes.domain.DisciplineCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisciplineCatalogRepository extends JpaRepository<DisciplineCatalog, UUID> {
    List<DisciplineCatalog> findByActiveTrueOrderByCategoryAscSortOrderAsc();
    boolean existsByName(String name);
}
