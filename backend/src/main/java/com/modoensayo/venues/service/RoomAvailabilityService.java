package com.modoensayo.venues.service;

import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.venues.dto.RoomAvailabilityRequest;
import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import com.modoensayo.venues.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * TODO: RoomAvailability entity and repository have been removed.
 * Reimplement this service using VenueScheduleService when ready.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAvailabilityService {

    private final RoomRepository roomRepository;
    private final ClassRepository classRepository;

    @Transactional
    public RoomAvailabilityResponse createAvailability(String venueAdminId, RoomAvailabilityRequest request) {
        throw new UnsupportedOperationException("RoomAvailability removed. TODO: use VenueScheduleService");
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailabilityByRoom(String roomId) {
        log.warn("RoomAvailabilityService.getAvailabilityByRoom: RoomAvailability removed. Returning empty list.");
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailabilityByRoomForAdmin(String venueAdminId, String roomId) {
        log.warn("RoomAvailabilityService.getAvailabilityByRoomForAdmin: RoomAvailability removed. Returning empty list.");
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailableSlotsForReschedule(String venueId, Instant afterTime) {
        log.warn("RoomAvailabilityService.getAvailableSlotsForReschedule: RoomAvailability removed. Returning empty list.");
        return Collections.emptyList();
    }

    @Transactional
    public void deleteAvailability(String venueAdminId, String availabilityId) {
        throw new UnsupportedOperationException("RoomAvailability removed. TODO: use VenueScheduleService");
    }
}
