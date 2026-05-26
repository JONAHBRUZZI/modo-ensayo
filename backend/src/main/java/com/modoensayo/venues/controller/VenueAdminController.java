package com.modoensayo.venues.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venue-admin")
@RequiredArgsConstructor
public class VenueAdminController {

    private final VenueService venueService;

    @GetMapping("/my-venues")
    public ResponseEntity<List<VenueResponse>> getMyVenues(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(venueService.getMyVenues(user.getUserId()));
    }

    @PostMapping("/venues")
    public ResponseEntity<VenueResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.createVenueAdmin(user.getUserId(), req));
    }

    @PatchMapping("/venues/{id}")
    public ResponseEntity<VenueResponse> update(@PathVariable UUID id, @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.updateVenue(id, req));
    }

    @GetMapping("/venues/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getRooms(venueId));
    }

    @PostMapping("/venues/{venueId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(@PathVariable UUID venueId, @RequestBody RoomRequest req) {
        return ResponseEntity.ok(venueService.createRoom(venueId, req));
    }

    @PostMapping("/rooms/{roomId}/availability")
    public ResponseEntity<RoomAvailabilityResponse> createAvailability(@PathVariable UUID roomId,
                                                                        @RequestBody RoomAvailabilityRequest req) {
        return ResponseEntity.ok(venueService.createAvailability(roomId, req));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailability(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venueService.getRoomAvailability(roomId));
    }

    @PostMapping("/rooms/{roomId}/availability/delete/{slotId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable UUID roomId, @PathVariable UUID slotId) {
        venueService.deleteAvailability(slotId);
        return ResponseEntity.noContent().build();
    }
}
