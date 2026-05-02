package com.modoensayo.venues.controller;

import com.modoensayo.venues.dto.RoomRequest;
import com.modoensayo.venues.dto.RoomResponse;
import com.modoensayo.venues.dto.VenueRequest;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<List<VenueResponse>> listApproved() {
        return ResponseEntity.ok(venueService.listApproved());
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.create(userDetails.getUsername(), request));
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
}
