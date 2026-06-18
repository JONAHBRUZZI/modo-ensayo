package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.VenueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VenueScheduleRepository extends JpaRepository<VenueSchedule, UUID> {
    List<VenueSchedule> findByVenueId(UUID venueId);
    void deleteByVenueId(UUID venueId);
}
