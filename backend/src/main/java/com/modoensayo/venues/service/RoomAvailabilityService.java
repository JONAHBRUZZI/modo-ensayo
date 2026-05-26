package com.modoensayo.venues.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.RoomAvailability;
import com.modoensayo.venues.dto.RoomAvailabilityRequest;
import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import com.modoensayo.venues.repository.RoomAvailabilityRepository;
import com.modoensayo.venues.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAvailabilityService {

    private final RoomAvailabilityRepository availabilityRepository;
    private final RoomRepository roomRepository;
    private final ClassRepository classRepository;

    @Transactional
    public RoomAvailabilityResponse createAvailability(String venueAdminId, RoomAvailabilityRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!room.getVenue().getAdminId().equals(UUID.fromString(venueAdminId))) {
            throw new BusinessException("No tienes permiso para gestionar esta sala");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }

        List<RoomAvailability> conflicts = availabilityRepository
                .findByRoomIdAndStartTimeBeforeAndEndTimeAfter(room.getId(), request.endTime(), request.startTime());
        if (!conflicts.isEmpty()) {
            throw new BusinessException("Ya existe un bloque de disponibilidad que se superpone con este horario");
        }

        List<Class> scheduledClasses = classRepository.findByStatusAndRoomVenueId(ClassStatus.PUBLISHED, room.getVenue().getId())
                .stream()
                .filter(c -> c.getRoom().getId().equals(room.getId())
                        && c.getStartTime().isBefore(request.endTime())
                        && c.getEndTime().isAfter(request.startTime()))
                .toList();
        if (!scheduledClasses.isEmpty()) {
            throw new BusinessException("Ya existe una clase programada en ese horario");
        }

        RoomAvailability availability = RoomAvailability.builder()
                .room(room)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        availabilityRepository.save(availability);
        log.info("Created availability for room {} from {} to {}", room.getId(), request.startTime(), request.endTime());
        return toResponse(availability);
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailabilityByRoom(String roomId) {
        return availabilityRepository.findByRoomIdAndEndTimeAfter(UUID.fromString(roomId), Instant.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailabilityByRoomForAdmin(String venueAdminId, String roomId) {
        Room room = roomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (!room.getVenue().getAdminId().equals(UUID.fromString(venueAdminId))) {
            throw new BusinessException("No tienes permiso para ver esta sala");
        }
        return availabilityRepository.findByRoomId(UUID.fromString(roomId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailableSlotsForReschedule(String venueId, Instant afterTime) {
        List<Room> rooms = roomRepository.findByVenueId(UUID.fromString(venueId));
        List<RoomAvailabilityResponse> result = new java.util.ArrayList<>();

        for (Room room : rooms) {
            List<RoomAvailability> availabilities = availabilityRepository
                    .findByRoomIdAndEndTimeAfter(room.getId(), afterTime);

            for (RoomAvailability avail : availabilities) {
                if (avail.getStartTime().isAfter(afterTime) || avail.getStartTime().equals(afterTime)) {
                    result.add(toResponse(avail));
                }
            }
        }
        return result;
    }

    @Transactional
    public void deleteAvailability(String venueAdminId, String availabilityId) {
        RoomAvailability availability = availabilityRepository.findById(UUID.fromString(availabilityId))
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (!availability.getRoom().getVenue().getAdminId().equals(UUID.fromString(venueAdminId))) {
            throw new BusinessException("No tienes permiso para eliminar este bloque");
        }

        availabilityRepository.delete(availability);
        log.info("Deleted availability slot {}", availabilityId);
    }

    private RoomAvailabilityResponse toResponse(RoomAvailability avail) {
        return new RoomAvailabilityResponse(
                avail.getId().toString(),
                avail.getRoom().getId().toString(),
                avail.getRoom().getName(),
                avail.getStartTime(),
                avail.getEndTime()
        );
    }
}
