package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;

    public List<ClassResponse> listPublished() {
        return classRepository.findByStatusOrderByStartTimeAsc(ClassStatus.PUBLISHED).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse create(ClassRequest req) {
        Room room = roomRepository.findById(req.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        Class c = Class.builder()
                .title(req.title())
                .discipline(req.discipline() != null ? Disciplina.valueOf(req.discipline()) : Disciplina.OTRO)
                .level(req.level() != null ? NivelClase.valueOf(req.level()) : NivelClase.BASICO)
                .description(req.description())
                .capacity(req.capacity())
                .duration(req.duration())
                .price(req.price())
                .minAge(req.minAge() != null ? req.minAge() : 0)
                .maxAge(req.maxAge() != null ? req.maxAge() : 99)
                .startTime(req.startTime())
                .room(room)
                .status(ClassStatus.PUBLISHED)
                .build();

        return toResponse(classRepository.save(c));
    }

    public List<ClassResponse> getTeacherClasses(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClassResponse> getByVenue(UUID venueId) {
        return classRepository.findByRoomVenueId(venueId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse updateStatus(UUID classId, ClassStatus status) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        c.setStatus(status);
        return toResponse(classRepository.save(c));
    }

    private ClassResponse toResponse(Class c) {
        return new ClassResponse(c.getId(), c.getTitle(),
                c.getDiscipline() != null ? c.getDiscipline().name() : null,
                c.getLevel() != null ? c.getLevel().name() : null,
                c.getDescription(), c.getCapacity(), c.getDuration(), c.getPrice(),
                c.getMinAge(), c.getMaxAge(), c.getStartTime(),
                c.getRoom() != null ? c.getRoom().getId() : null,
                c.getRoom() != null ? c.getRoom().getName() : null,
                c.getRoom() != null && c.getRoom().getVenue() != null ? c.getRoom().getVenue().getId() : null,
                c.getRoom() != null && c.getRoom().getVenue() != null ? c.getRoom().getVenue().getName() : null,
                c.getTeacherId(), c.getStatus() != null ? c.getStatus().name() : null,
                0, c.getCreatedAt());
    }
}
