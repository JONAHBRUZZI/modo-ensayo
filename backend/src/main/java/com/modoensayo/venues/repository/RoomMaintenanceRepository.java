package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.RoomMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomMaintenanceRepository extends JpaRepository<RoomMaintenance, UUID> {
    List<RoomMaintenance> findByRoomId(UUID roomId);
}
