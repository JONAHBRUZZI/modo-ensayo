package com.modoensayo.venues.controller;

import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import com.modoensayo.venues.dto.RoomRequest;
import com.modoensayo.venues.dto.RoomResponse;
import com.modoensayo.venues.dto.VenueRequest;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.service.RoomAvailabilityService;
import com.modoensayo.venues.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final RoomAvailabilityService roomAvailabilityService;

    @GetMapping
    public ResponseEntity<List<VenueResponse>> listApproved() {
        return ResponseEntity.ok(venueService.listApproved());
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.create(com.modoensayo.shared.security.SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable String id) {
        return ResponseEntity.ok(venueService.getRooms(id));
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createRoom(request));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getRoomAvailability(
            @PathVariable String roomId,
            @RequestParam(required = false) Instant afterTime) {
        Instant time = afterTime != null ? afterTime : Instant.now();
        return ResponseEntity.ok(roomAvailabilityService.getAvailabilityByRoom(roomId)
                .stream()
                .filter(a -> a.startTime().isAfter(time) || a.startTime().equals(time))
                .toList());
    }
}
