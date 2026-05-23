package com.modoensayo.venues.service;

import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.*;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;

    public List<VenueResponse> listApproved() {
        return venueRepository.findByStatusOrderByCreatedAtDesc("APROBADA").stream()
                .map(this::toVenueResponse).collect(Collectors.toList());
    }

    @Transactional
    public VenueResponse create(VenueRequest req) {
        Venue v = Venue.builder()
                .name(req.name()).city(req.city()).address(req.address())
                .description(req.description()).phone(req.phone()).email(req.email())
                .status(EstadoSede.PENDIENTE_APROBACION).build();
        return toVenueResponse(venueRepository.save(v));
    }

    public List<VenueResponse> getMyVenues(UUID adminId) {
        return venueRepository.findByAdminId(adminId).stream()
                .map(this::toVenueResponse).collect(Collectors.toList());
    }

    @Transactional
    public VenueResponse createVenueAdmin(UUID adminId, VenueRequest req) {
        Venue v = Venue.builder()
                .adminId(adminId).name(req.name()).city(req.city()).address(req.address())
                .description(req.description()).phone(req.phone()).email(req.email())
                .status(EstadoSede.APROBADA).build();
        return toVenueResponse(venueRepository.save(v));
    }

    @Transactional
    public VenueResponse updateVenue(UUID venueId, VenueRequest req) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (req.name() != null) v.setName(req.name());
        if (req.city() != null) v.setCity(req.city());
        if (req.address() != null) v.setAddress(req.address());
        if (req.description() != null) v.setDescription(req.description());
        if (req.phone() != null) v.setPhone(req.phone());
        if (req.email() != null) v.setEmail(req.email());
        return toVenueResponse(venueRepository.save(v));
    }

    public List<RoomResponse> getRooms(UUID venueId) {
        return roomRepository.findByVenueId(venueId).stream()
                .map(this::toRoomResponse).collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse createRoom(UUID venueId, RoomRequest req) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        Room r = Room.builder().venue(v).name(req.name()).capacity(req.capacity())
                .floorType(req.floorType()).type(req.type()).hasMirrors(req.hasMirrors())
                .hasSound(req.hasSound()).equipment(req.equipment()).build();
        return toRoomResponse(roomRepository.save(r));
    }

    public List<RoomAvailabilityResponse> getRoomAvailability(UUID roomId) {
        return roomAvailabilityRepository.findByRoomId(roomId).stream()
                .map(a -> new RoomAvailabilityResponse(a.getId(), a.getRoom().getId(), a.getStartTime(), a.getEndTime()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomAvailabilityResponse createAvailability(UUID roomId, RoomAvailabilityRequest req) {
        Room r = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        RoomAvailability a = RoomAvailability.builder().room(r).startTime(req.startTime()).endTime(req.endTime()).build();
        a = roomAvailabilityRepository.save(a);
        return new RoomAvailabilityResponse(a.getId(), a.getRoom().getId(), a.getStartTime(), a.getEndTime());
    }

    @Transactional
    public void deleteAvailability(UUID availId) {
        roomAvailabilityRepository.deleteById(availId);
    }

    private VenueResponse toVenueResponse(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(), v.getCreatedAt());
    }

    private RoomResponse toRoomResponse(Room r) {
        return new RoomResponse(r.getId(), r.getVenue().getId(), r.getVenue().getName(),
                r.getName(), r.getCapacity(), r.getFloorType(), r.getType(), r.getEquipment(), r.getCreatedAt());
    }
}
