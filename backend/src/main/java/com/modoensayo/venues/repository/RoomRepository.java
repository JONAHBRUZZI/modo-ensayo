package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByVenueId(UUID venueId);
}
