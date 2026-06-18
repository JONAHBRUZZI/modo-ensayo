package com.modoensayo.venues.controller;

import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.RoomScheduleBlock;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomScheduleController {
    private final VenueScheduleService scheduleService;
    private final RoomRepository roomRepo;

    @GetMapping("/available")
    public ResponseEntity<List<RoomScheduleBlock>> searchAvailable(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant f = from != null ? Instant.parse(from) : Instant.now();
        Instant t = to != null ? Instant.parse(to) : Instant.now().plus(14, java.time.temporal.ChronoUnit.DAYS);
        List<UUID> allRoomIds = roomRepo.findAll().stream().map(Room::getId).toList();
        if (allRoomIds.isEmpty()) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(scheduleService.getRoomsSchedule(allRoomIds, f, t)
                .stream().filter(b -> "AVAILABLE".equals(b.getStatus())).toList());
    }

    @PostMapping("/{roomId}/book")
    public ResponseEntity<RoomScheduleBlock> bookSlot(@PathVariable UUID roomId,
            @RequestBody Map<String, String> body) {
        UUID blockId = UUID.fromString(body.get("blockId"));
        UUID classId = body.get("classId") != null ? UUID.fromString(body.get("classId")) : null;
        return ResponseEntity.ok(scheduleService.bookSlot(blockId, classId));
    }
}
