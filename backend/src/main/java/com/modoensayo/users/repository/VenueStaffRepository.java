package com.modoensayo.users.repository;

import com.modoensayo.users.domain.VenueStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VenueStaffRepository extends JpaRepository<VenueStaff, UUID> {
    List<VenueStaff> findByUser_Id(UUID userId);
    List<VenueStaff> findByVenueId(UUID venueId);
}
