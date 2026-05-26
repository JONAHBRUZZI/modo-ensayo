package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.RoomAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, UUID> {
    List<RoomAvailability> findByRoomId(UUID roomId);
    List<RoomAvailability> findByRoomIdAndEndTimeAfter(UUID roomId, Instant after);
    List<RoomAvailability> findByRoomIdAndStartTimeBeforeAndEndTimeAfter(UUID roomId, Instant startTime, Instant endTime);
}
