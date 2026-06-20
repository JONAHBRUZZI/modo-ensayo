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
    @Query("SELECT b FROM RoomScheduleBlock b JOIN FETCH b.room r WHERE r.id = :roomId AND b.startTime BETWEEN :from AND :to ORDER BY b.startTime")
    List<RoomScheduleBlock> findByRoomIdAndStartTimeBetweenOrderByStartTime(@Param("roomId") UUID roomId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT b FROM RoomScheduleBlock b JOIN FETCH b.room r WHERE r.id = :roomId AND b.status = :status AND b.startTime BETWEEN :from AND :to ORDER BY b.startTime")
    List<RoomScheduleBlock> findByRoomIdAndStatusAndStartTimeBetweenOrderByStartTime(@Param("roomId") UUID roomId, @Param("status") String status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT b FROM RoomScheduleBlock b JOIN FETCH b.room r WHERE r.id IN :roomIds AND b.startTime BETWEEN :from AND :to ORDER BY b.startTime")
    List<RoomScheduleBlock> findByRoomIdInAndStartTimeBetweenOrderByStartTime(@Param("roomIds") List<UUID> roomIds, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT b FROM RoomScheduleBlock b JOIN FETCH b.room r WHERE r.id IN :roomIds AND b.status = :status AND b.startTime BETWEEN :from AND :to ORDER BY b.startTime")
    List<RoomScheduleBlock> findByRoomIdInAndStatusAndStartTimeBetweenOrderByStartTime(@Param("roomIds") List<UUID> roomIds, @Param("status") String status, @Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("DELETE FROM RoomScheduleBlock r WHERE r.room.id IN :roomIds AND r.status = 'AVAILABLE'")
    void deleteAvailableByRoomIds(@Param("roomIds") List<UUID> roomIds);

    List<RoomScheduleBlock> findByClassId(UUID classId);
}
