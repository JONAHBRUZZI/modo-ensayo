package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.VenueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface VenueScheduleRepository extends JpaRepository<VenueSchedule, UUID> {
    List<VenueSchedule> findByVenueId(UUID venueId);

    // Bulk delete que se ejecuta de inmediato, antes de los inserts del saveAll,
    // para no chocar con la constraint unica (venue_id, day_of_week).
    @Modifying
    @Query("DELETE FROM VenueSchedule s WHERE s.venueId = :venueId")
    void deleteByVenueId(@Param("venueId") UUID venueId);
}
