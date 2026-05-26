package com.modoensayo.venues.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.service.ClassConfirmationService;
import com.modoensayo.venues.service.ClassConfirmationService.ClassConfirmationResult;
import com.modoensayo.venues.service.ClassConfirmationService.ClassSummaryDto;
import com.modoensayo.venues.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/venue-admin")
@RequiredArgsConstructor
public class VenueAdminController {

    private final VenueService venueService;
    private final ClassConfirmationService classConfirmationService;

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
    public ResponseEntity<VenueResponse> update(@AuthenticationPrincipal CustomUserDetails user,
                                                 @PathVariable UUID id, @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.updateVenue(user.getUserId(), id, req));
    }

    @GetMapping("/venues/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getRooms(venueId));
    }

    @PostMapping("/venues/{venueId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(@AuthenticationPrincipal CustomUserDetails user,
                                                    @PathVariable UUID venueId, @RequestBody RoomRequest req) {
        return ResponseEntity.ok(venueService.createRoom(user.getUserId(), venueId, req));
    }

    @PostMapping("/rooms/{roomId}/availability")
    public ResponseEntity<RoomAvailabilityResponse> createAvailability(@AuthenticationPrincipal CustomUserDetails user,
                                                                        @PathVariable UUID roomId,
                                                                        @RequestBody RoomAvailabilityRequest req) {
        return ResponseEntity.ok(venueService.createAvailability(user.getUserId(), roomId, req));
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAvailability(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venueService.getRoomAvailability(roomId));
    }

    @DeleteMapping("/rooms/{roomId}/availability/{slotId}")
    public ResponseEntity<Void> deleteAvailability(@AuthenticationPrincipal CustomUserDetails user,
                                                    @PathVariable UUID roomId, @PathVariable UUID slotId) {
        venueService.deleteAvailability(user.getUserId(), slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClassSummaryDto>> getVenueClasses(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
            classConfirmationService.getAllVenueClasses(user.getUserId().toString()));
    }

    @GetMapping("/classes/pending-confirmation")
    public ResponseEntity<List<ClassSummaryDto>> getPendingClasses(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
            classConfirmationService.getClassesPendingConfirmation(user.getUserId().toString()));
    }

    @PostMapping("/classes/{classId}/confirm-realized")
    public ResponseEntity<ClassConfirmationResult> confirmRealized(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String classId,
            @RequestBody Map<String, Object> body) {
        Object confirm = body.get("confirmacion");
        if (confirm == null || !Boolean.TRUE.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            classConfirmationService.confirmClassRealized(user.getUserId().toString(), classId));
    }

    @PostMapping("/classes/{classId}/confirm-not-realized")
    public ResponseEntity<ClassConfirmationResult> confirmNotRealized(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String classId,
            @RequestBody Map<String, Object> body) {
        Object confirm = body.get("confirmacion");
        if (confirm == null || !Boolean.TRUE.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            classConfirmationService.confirmClassNotRealized(user.getUserId().toString(), classId));
    }
}
