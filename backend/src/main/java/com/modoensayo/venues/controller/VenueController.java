package com.modoensayo.venues.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.venues.domain.VenuePhoto;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.repository.VenuePhotoRepository;
import com.modoensayo.venues.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final VenuePhotoRepository venuePhotoRepository;

    @GetMapping
    public ResponseEntity<List<VenueResponse>> listApproved() {
        return ResponseEntity.ok(venueService.listApproved());
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.createVenueWithIdentityValidation(user.getUserId(), req));
    }

    @GetMapping("/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getRooms(venueId));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailability(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venueService.getRoomAvailability(roomId));
    }

    /** Fotos públicas de una sede. */
    @GetMapping("/{venueId}/fotos")
    public ResponseEntity<List<VenuePhoto>> getVenuePhotos(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venuePhotoRepository
                .findByOwnerIdAndOwnerTypeOrderByDisplayOrderAsc(venueId, "VENUE"));
    }

    /** Fotos públicas de una sala. */
    @GetMapping("/rooms/{roomId}/fotos")
    public ResponseEntity<List<VenuePhoto>> getRoomPhotos(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venuePhotoRepository
                .findByOwnerIdAndOwnerTypeOrderByDisplayOrderAsc(roomId, "ROOM"));
    }
}
