package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.VenueDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenueDocumentRepository extends JpaRepository<VenueDocument, UUID> {
    List<VenueDocument> findByVenueId(UUID venueId);
    List<VenueDocument> findByVenueIdOrderByCreatedAtDesc(UUID venueId);
    long countByVenueIdAndEstado(UUID venueId, String estado);
}
