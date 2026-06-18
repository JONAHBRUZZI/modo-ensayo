package com.modoensayo.venues.service;

import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.RoomScheduleBlock;
import com.modoensayo.venues.domain.VenueBlockConfig;
import com.modoensayo.venues.domain.VenueSchedule;
import com.modoensayo.venues.repository.RoomRepository;
import com.modoensayo.venues.repository.RoomScheduleBlockRepository;
import com.modoensayo.venues.repository.VenueBlockConfigRepository;
import com.modoensayo.venues.repository.VenueScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueScheduleService {
    private final VenueScheduleRepository scheduleRepo;
    private final VenueBlockConfigRepository blockConfigRepo;
    private final RoomScheduleBlockRepository blockRepo;
    private final RoomRepository roomRepo;

    @Transactional
    public List<VenueSchedule> saveSchedule(UUID venueId, List<VenueSchedule> schedules) {
        scheduleRepo.deleteByVenueId(venueId);
        return scheduleRepo.saveAll(schedules);
    }

    public List<VenueSchedule> getSchedule(UUID venueId) {
        return scheduleRepo.findByVenueId(venueId);
    }

    @Transactional
    public VenueBlockConfig saveBlockConfig(UUID venueId, Integer blockDurationMin, Integer gapBetweenBlocksMin) {
        VenueBlockConfig config = blockConfigRepo.findByVenueId(venueId)
                .orElse(VenueBlockConfig.builder().venueId(venueId).build());
        config.setBlockDurationMin(blockDurationMin != null ? blockDurationMin : 60);
        config.setGapBetweenBlocksMin(gapBetweenBlocksMin != null ? gapBetweenBlocksMin : 15);
        return blockConfigRepo.save(config);
    }

    public VenueBlockConfig getBlockConfig(UUID venueId) {
        return blockConfigRepo.findByVenueId(venueId)
                .orElse(VenueBlockConfig.builder().venueId(venueId)
                        .blockDurationMin(60).gapBetweenBlocksMin(15).build());
    }

    @Transactional
    public void generateBlocks(UUID venueId) {
        List<Room> rooms = roomRepo.findByVenueId(venueId);
        List<VenueSchedule> schedules = scheduleRepo.findByVenueId(venueId);
        VenueBlockConfig config = getBlockConfig(venueId);

        List<UUID> roomIds = rooms.stream().map(Room::getId).toList();
        blockRepo.deleteAvailableByRoomIds(roomIds);

        Instant now = Instant.now();
        Instant endWindow = Instant.now().plus(30, ChronoUnit.DAYS);

        for (Room room : rooms) {
            for (VenueSchedule sched : schedules) {
                generateDayBlocks(room, sched, config, now, endWindow);
            }
        }
    }

    private void generateDayBlocks(Room room, VenueSchedule sched, VenueBlockConfig config, Instant from, Instant to) {
        LocalDate start = LocalDate.now();
        DayOfWeek targetDay = DayOfWeek.valueOf(sched.getDayOfWeek());

        LocalDate next = start;
        while (next.getDayOfWeek() != targetDay) next = next.plusDays(1);

        ZoneId santiago = ZoneId.of("America/Santiago");

        while (next.isBefore(LocalDate.now().plusDays(30))) {
            Instant dayStart = next.atTime(sched.getOpenTime()).atZone(santiago).toInstant();
            Instant dayEnd = next.atTime(sched.getCloseTime()).atZone(santiago).toInstant();
            Instant current = dayStart;

            while (current.plus(config.getBlockDurationMin(), ChronoUnit.MINUTES).isBefore(dayEnd)
                    || current.plus(config.getBlockDurationMin(), ChronoUnit.MINUTES).equals(dayEnd)) {
                Instant blockEnd = current.plus(config.getBlockDurationMin(), ChronoUnit.MINUTES);

                List<RoomScheduleBlock> existing = blockRepo.findByRoomIdAndStartTimeBetweenOrderByStartTime(
                        room.getId(), current, blockEnd);
                if (existing.isEmpty()) {
                    RoomScheduleBlock block = RoomScheduleBlock.builder()
                            .room(room).startTime(current).endTime(blockEnd).status("AVAILABLE").build();
                    blockRepo.save(block);
                }

                current = blockEnd.plus(config.getGapBetweenBlocksMin(), ChronoUnit.MINUTES);
            }
            next = next.plusWeeks(1);
        }
    }

    public List<RoomScheduleBlock> getRoomSchedule(UUID roomId, Instant from, Instant to) {
        return blockRepo.findByRoomIdAndStartTimeBetweenOrderByStartTime(roomId, from, to);
    }

    public List<RoomScheduleBlock> getRoomsSchedule(List<UUID> roomIds, Instant from, Instant to) {
        return blockRepo.findByRoomIdInAndStartTimeBetweenOrderByStartTime(roomIds, from, to);
    }

    @Transactional
    public RoomScheduleBlock markMaintenance(UUID blockId, String reason) {
        RoomScheduleBlock block = blockRepo.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));
        if (!"AVAILABLE".equals(block.getStatus())) {
            throw new BusinessException("Solo bloques disponibles pueden marcarse como mantencion");
        }
        block.setStatus("MAINTENANCE");
        return blockRepo.save(block);
    }

    @Transactional
    public RoomScheduleBlock releaseMaintenance(UUID blockId) {
        RoomScheduleBlock block = blockRepo.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));
        if (!"MAINTENANCE".equals(block.getStatus())) {
            throw new BusinessException("Solo bloques en mantencion pueden liberarse");
        }
        block.setStatus("AVAILABLE");
        return blockRepo.save(block);
    }

    @Transactional
    public RoomScheduleBlock bookSlot(UUID blockId, UUID classId) {
        RoomScheduleBlock block = blockRepo.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));
        if (!"AVAILABLE".equals(block.getStatus())) {
            throw new BusinessException("Este horario ya no esta disponible");
        }
        block.setStatus("OCCUPIED");
        block.setClassId(classId);
        return blockRepo.save(block);
    }

    @Transactional
    public void releaseSlot(UUID blockId) {
        blockRepo.findById(blockId).ifPresent(block -> {
            block.setStatus("AVAILABLE");
            block.setClassId(null);
            blockRepo.save(block);
        });
    }
}
