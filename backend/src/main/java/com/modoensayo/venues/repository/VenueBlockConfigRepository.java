package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.VenueBlockConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VenueBlockConfigRepository extends JpaRepository<VenueBlockConfig, UUID> {
    Optional<VenueBlockConfig> findByVenueId(UUID venueId);
}
