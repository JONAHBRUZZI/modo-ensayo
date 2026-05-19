package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.Equipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EquipamientoRepository extends JpaRepository<Equipamiento, UUID> {
    List<Equipamiento> findByRoom_Id(UUID roomId);
    List<Equipamiento> findByTipoIgnoreCase(String tipo);
}
