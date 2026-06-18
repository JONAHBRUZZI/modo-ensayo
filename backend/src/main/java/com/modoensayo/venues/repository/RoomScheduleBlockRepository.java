package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.RoomScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoomScheduleBlockRepository extends JpaRepository<RoomScheduleBlock, UUID> {
    List<RoomScheduleBlock> findByRoomIdAndStartTimeBetweenOrderByStartTime(UUID roomId, Instant from, Instant to);
    List<RoomScheduleBlock> findByRoomIdAndStatusAndStartTimeBetweenOrderByStartTime(UUID roomId, String status, Instant from, Instant to);
    List<RoomScheduleBlock> findByRoomIdInAndStartTimeBetweenOrderByStartTime(List<UUID> roomIds, Instant from, Instant to);
    List<RoomScheduleBlock> findByRoomIdInAndStatusAndStartTimeBetweenOrderByStartTime(List<UUID> roomIds, String status, Instant from, Instant to);

    @Modifying
    @Query("DELETE FROM RoomScheduleBlock r WHERE r.room.id IN :roomIds AND r.status = 'AVAILABLE'")
    void deleteAvailableByRoomIds(@Param("roomIds") List<UUID> roomIds);

    List<RoomScheduleBlock> findByClassId(UUID classId);
}
