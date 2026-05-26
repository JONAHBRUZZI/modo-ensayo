package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findByStatusOrderByCreatedAtDesc(EstadoSede status);
    List<Venue> findByAdminId(UUID adminId);
    long countByStatus(EstadoSede status);
}
