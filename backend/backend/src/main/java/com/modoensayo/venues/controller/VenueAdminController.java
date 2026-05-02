package com.modoensayo.venues.controller;

import com.modoensayo.shared.security.SecurityUtils;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.dto.RoomAvailabilityRequest;
import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import com.modoensayo.venues.dto.RoomRequest;
import com.modoensayo.venues.dto.RoomResponse;
import com.modoensayo.venues.dto.VenueRequest;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.service.ClassConfirmationService;
import com.modoensayo.venues.service.RoomAvailabilityService;
import com.modoensayo.venues.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/venue-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENUE_ADMIN', 'ADMIN')")
public class VenueAdminController {

    private final VenueService venueService;
    private final ClassConfirmationService classConfirmationService;
    private final RoomAvailabilityService roomAvailabilityService;

    @GetMapping("/my-venues")
    public ResponseEntity<List<VenueResponse>> getMyVenues() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.getVenuesByAdminId(userId));
    }

    @PostMapping("/venues")
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody VenueRequest request) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.create(adminId, request));
    }

    @PatchMapping("/venues/{id}")
    public ResponseEntity<VenueResponse> updateVenue(@PathVariable String id, @Valid @RequestBody VenueRequest request) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.updateVenue(id, adminId, request));
    }

    @GetMapping("/venues/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable String venueId) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.getRoomsForAdmin(venueId, adminId));
    }

    @PostMapping("/venues/{venueId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(@PathVariable String venueId, @Valid @RequestBody RoomRequest request) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.createRoomForAdmin(venueId, adminId, request));
    }

    @GetMapping("/classes/pending-confirmation")
    public ResponseEntity<List<ClassConfirmationService.ClassSummaryDto>> getClassesPendingConfirmation() {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(classConfirmationService.getClassesPendingConfirmation(adminId));
    }

    @PatchMapping("/classes/{classId}/confirm-realized")
    public ResponseEntity<ClassConfirmationService.ClassConfirmationResult> confirmClassRealized(@PathVariable String classId) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(classConfirmationService.confirmClassRealized(adminId, classId));
    }

    @PatchMapping("/classes/{classId}/confirm-not-realized")
    public ResponseEntity<ClassConfirmationService.ClassConfirmationResult> confirmClassNotRealized(@PathVariable String classId) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(classConfirmationService.confirmClassNotRealized(adminId, classId));
    }

    @PostMapping("/rooms/{roomId}/availability")
    public ResponseEntity<RoomAvailabilityResponse> createAvailability(@PathVariable String roomId, @Valid @RequestBody RoomAvailabilityRequest request) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(roomAvailabilityService.createAvailability(adminId, request));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getRoomAvailability(@PathVariable String roomId) {
        String adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(roomAvailabilityService.getAvailabilityByRoomForAdmin(adminId, roomId));
    }

    @PostMapping("/rooms/{roomId}/availability/delete/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable String roomId, @PathVariable String availabilityId) {
        String adminId = SecurityUtils.getCurrentUserId();
        roomAvailabilityService.deleteAvailability(adminId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
