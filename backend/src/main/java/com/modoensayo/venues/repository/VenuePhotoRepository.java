package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.VenuePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenuePhotoRepository extends JpaRepository<VenuePhoto, UUID> {
    List<VenuePhoto> findByOwnerIdAndOwnerTypeOrderByDisplayOrderAsc(UUID ownerId, String ownerType);
    List<VenuePhoto> findByOwnerIdOrderByDisplayOrderAsc(UUID ownerId);
}
