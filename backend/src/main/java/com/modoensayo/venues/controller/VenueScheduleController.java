package com.modoensayo.venues.controller;

import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.VenueBlockConfig;
import com.modoensayo.venues.domain.VenueSchedule;
import com.modoensayo.venues.dto.RoomScheduleBlockDto;
import com.modoensayo.venues.repository.RoomRepository;
import com.modoensayo.venues.service.VenueScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueScheduleController {
    private final VenueScheduleService scheduleService;
    private final RoomRepository roomRepo;

    @PutMapping("/{venueId}/schedule")
    public ResponseEntity<List<VenueSchedule>> saveSchedule(@PathVariable UUID venueId,
                                                             @RequestBody List<VenueSchedule> schedules) {
        for (VenueSchedule s : schedules) s.setVenueId(venueId);
        return ResponseEntity.ok(scheduleService.saveSchedule(venueId, schedules));
    }

    @GetMapping("/{venueId}/schedule")
    public ResponseEntity<List<VenueSchedule>> getSchedule(@PathVariable UUID venueId) {
        return ResponseEntity.ok(scheduleService.getSchedule(venueId));
    }

    @PutMapping("/{venueId}/block-config")
    public ResponseEntity<VenueBlockConfig> saveBlockConfig(@PathVariable UUID venueId,
                                                             @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(scheduleService.saveBlockConfig(venueId,
                body.get("blockDurationMin"), body.get("gapBetweenBlocksMin")));
    }

    @GetMapping("/{venueId}/block-config")
    public ResponseEntity<VenueBlockConfig> getBlockConfig(@PathVariable UUID venueId) {
        return ResponseEntity.ok(scheduleService.getBlockConfig(venueId));
    }

    @PostMapping("/{venueId}/generate-blocks")
    public ResponseEntity<Void> generateBlocks(@PathVariable UUID venueId) {
        scheduleService.generateBlocks(venueId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{venueId}/rooms/schedule")
    public ResponseEntity<List<RoomScheduleBlockDto>> getVenueSchedule(
            @PathVariable UUID venueId, @RequestParam String from, @RequestParam String to) {
        List<UUID> roomIds = roomRepo.findByVenueId(venueId).stream().map(Room::getId).toList();
        return ResponseEntity.ok(scheduleService.getRoomsSchedule(roomIds, Instant.parse(from), Instant.parse(to)));
    }

    @GetMapping("/rooms/{roomId}/schedule")
    public ResponseEntity<List<RoomScheduleBlockDto>> getRoomSchedule(
            @PathVariable UUID roomId, @RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok(scheduleService.getRoomSchedule(roomId, Instant.parse(from), Instant.parse(to)));
    }

    @PostMapping("/rooms/{roomId}/blocks/{blockId}/maintenance")
    public ResponseEntity<RoomScheduleBlockDto> markMaintenance(@PathVariable UUID blockId,
                                                              @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(scheduleService.markMaintenance(blockId, body.getOrDefault("reason", "")));
    }

    @DeleteMapping("/rooms/blocks/{blockId}/maintenance")
    public ResponseEntity<RoomScheduleBlockDto> releaseMaintenance(@PathVariable UUID blockId) {
        return ResponseEntity.ok(scheduleService.releaseMaintenance(blockId));
    }
}
