package com.modoensayo.venues.controller;

import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<List<VenueResponse>> listApproved() {
        return ResponseEntity.ok(venueService.listApproved());
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.create(req));
    }

    @GetMapping("/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getRooms(venueId));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailability(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venueService.getRoomAvailability(roomId));
    }
}
