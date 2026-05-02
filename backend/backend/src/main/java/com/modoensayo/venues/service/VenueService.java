package com.modoensayo.venues.service;

import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.dto.RoomRequest;
import com.modoensayo.venues.dto.RoomResponse;
import com.modoensayo.venues.dto.VenueRequest;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.repository.RoomRepository;
import com.modoensayo.venues.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;

    public VenueService(VenueRepository venueRepository, RoomRepository roomRepository) {
        this.venueRepository = venueRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> listApproved() {
        return venueRepository.findByStatus("APPROVED").stream()
                .map(this::toVenueResponse)
                .toList();
    }

    @Transactional
    public VenueResponse create(String adminId, VenueRequest request) {
        Venue venue = Venue.builder()
                .adminId(UUID.fromString(adminId))
                .name(request.name())
                .address(request.address())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .phone(request.phone())
                .email(request.email())
                .status("PENDING")
                .build();

        venueRepository.save(venue);
        return toVenueResponse(venue);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms(String venueId) {
        return roomRepository.findByVenueId(UUID.fromString(venueId)).stream()
                .map(this::toRoomResponse)
                .toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Venue venue = venueRepository.findById(UUID.fromString(request.venueId()))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        Room room = Room.builder()
                .venue(venue)
                .name(request.name())
                .capacity(request.capacity())
                .floorType(request.floorType())
                .hasMirrors(request.hasMirrors() != null ? request.hasMirrors() : false)
                .hasSound(request.hasSound() != null ? request.hasSound() : false)
                .hasBalletBar(request.hasBalletBar() != null ? request.hasBalletBar() : false)
                .hasAirConditioning(request.hasAirConditioning() != null ? request.hasAirConditioning() : false)
                .hasNaturalLight(request.hasNaturalLight() != null ? request.hasNaturalLight() : false)
                .lighting(request.lighting())
                .wallColor(request.wallColor())
                .imageUrl(request.imageUrl())
                .build();

        roomRepository.save(room);
        return toRoomResponse(room);
    }

    private VenueResponse toVenueResponse(Venue venue) {
        return new VenueResponse(
                venue.getId().toString(),
                venue.getName(),
                venue.getAddress(),
                venue.getDescription(),
                venue.getImageUrl(),
                venue.getPhone(),
                venue.getEmail(),
                venue.getStatus()
        );
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId().toString(),
                room.getVenue().getId().toString(),
                room.getName(),
                room.getCapacity(),
                room.getFloorType(),
                room.getHasMirrors(),
                room.getHasSound(),
                room.getHasBalletBar(),
                room.getHasAirConditioning(),
                room.getHasNaturalLight(),
                room.getLighting(),
                room.getWallColor(),
                room.getImageUrl()
        );
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getVenuesByAdminId(String adminId) {
        return venueRepository.findByAdminId(UUID.fromString(adminId)).stream()
                .map(this::toVenueResponse)
                .toList();
    }

    @Transactional
    public VenueResponse updateVenue(String venueId, String adminId, VenueRequest request) {
        Venue venue = venueRepository.findById(UUID.fromString(venueId))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (!venue.getAdminId().equals(UUID.fromString(adminId))) {
            throw new BusinessException("No tienes permiso para editar esta sede");
        }
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setDescription(request.description());
        venue.setImageUrl(request.imageUrl());
        venue.setPhone(request.phone());
        venue.setEmail(request.email());
        venueRepository.save(venue);
        return toVenueResponse(venue);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsForAdmin(String venueId, String adminId) {
        Venue venue = venueRepository.findById(UUID.fromString(venueId))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (!venue.getAdminId().equals(UUID.fromString(adminId))) {
            throw new BusinessException("No tienes permiso para ver las salas de esta sede");
        }
        return getRooms(venueId);
    }

    @Transactional
    public RoomResponse createRoomForAdmin(String venueId, String adminId, RoomRequest request) {
        Venue venue = venueRepository.findById(UUID.fromString(venueId))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (!venue.getAdminId().equals(UUID.fromString(adminId))) {
            throw new BusinessException("No tienes permiso para crear salas en esta sede");
        }
        if (request.capacity() == null || request.capacity() <= 0) {
            throw new BusinessException("La capacidad debe ser mayor a 0");
        }
        Room room = Room.builder()
                .venue(venue)
                .name(request.name())
                .capacity(request.capacity())
                .floorType(request.floorType())
                .hasMirrors(request.hasMirrors() != null ? request.hasMirrors() : false)
                .hasSound(request.hasSound() != null ? request.hasSound() : false)
                .hasBalletBar(request.hasBalletBar() != null ? request.hasBalletBar() : false)
                .hasAirConditioning(request.hasAirConditioning() != null ? request.hasAirConditioning() : false)
                .hasNaturalLight(request.hasNaturalLight() != null ? request.hasNaturalLight() : false)
                .lighting(request.lighting())
                .wallColor(request.wallColor())
                .imageUrl(request.imageUrl())
                .build();
        roomRepository.save(room);
        return toRoomResponse(room);
    }
}
