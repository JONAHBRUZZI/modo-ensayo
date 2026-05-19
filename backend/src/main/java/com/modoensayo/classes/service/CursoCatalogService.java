package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.service.AttributeService;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.RoomRepository;
import com.modoensayo.venues.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class CursoCatalogService {

    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final AttributeService attributeService;

    public CursoCatalogService(ClassRepository classRepository,
                               RoomRepository roomRepository,
                               VenueRepository venueRepository,
                               UserRepository userRepository,
                               AttributeService attributeService) {
        this.classRepository = classRepository;
        this.roomRepository = roomRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.attributeService = attributeService;
    }

    @Transactional
    public Class createCursoPropia(Class curso, UUID maestroId) {
        if (curso.getRoom() == null || curso.getRoom().getId() == null) {
            throw new BusinessException("La sala es obligatoria");
        }

        Room room = roomRepository.findById(curso.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));

        Venue venue = room.getVenue();
        if (venue == null || !EstadoSede.APROBADA.name().equals(venue.getStatus())) {
            throw new BusinessException("La sede no esta aprobada");
        }

        if (curso.getCapacity() == null || curso.getCapacity() <= 0) {
            throw new BusinessException("La capacidad debe ser mayor a 0");
        }

        if (curso.getCapacity() > room.getCapacity()) {
            throw new BusinessException("La capacidad del curso excede la capacidad de la sala");
        }

        if (!userRepository.existsById(maestroId)) {
            throw new ResourceNotFoundException("Maestro no encontrado");
        }

        Class classEntity = Class.builder()
                .room(room)
                .teacherId(maestroId)
                .title(curso.getTitle())
                .discipline(curso.getDiscipline())
                .capacity(curso.getCapacity())
                .price(curso.getPrice())
                .startTime(curso.getStartTime())
                .endTime(curso.getEndTime())
                .status(ClassStatus.PUBLISHED)
                .build();

        Class saved = classRepository.save(classEntity);

        if (curso.getStartTime() != null && curso.getEndTime() != null) {
            LocalDateTime inicio = LocalDateTime.ofInstant(curso.getStartTime(), ZoneId.systemDefault());
            LocalDateTime fin = LocalDateTime.ofInstant(curso.getEndTime(), ZoneId.systemDefault());
            attributeService.promoverAMaestroIndependiente(
                    saved.getTeacherId().toString() + "@modoensayo.cl",
                    room.getId(),
                    inicio,
                    fin);
        }

        return saved;
    }

    @Transactional
    public Class createClaseAsignada(Class clase, UUID adminSedeId, UUID maestroDependienteId, String maestroEmail) {
        if (clase.getRoom() == null || clase.getRoom().getId() == null) {
            throw new BusinessException("La sala es obligatoria");
        }

        Room room = roomRepository.findById(clase.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));

        Venue venue = room.getVenue();
        if (venue == null || !EstadoSede.APROBADA.name().equals(venue.getStatus())) {
            throw new BusinessException("La sede no esta aprobada");
        }

        if (clase.getCapacity() == null || clase.getCapacity() <= 0) {
            throw new BusinessException("La capacidad debe ser mayor a 0");
        }

        if (!userRepository.existsById(maestroDependienteId)) {
            throw new ResourceNotFoundException("Maestro dependiente no encontrado");
        }

        Class classEntity = Class.builder()
                .room(room)
                .teacherId(maestroDependienteId)
                .title(clase.getTitle())
                .discipline(clase.getDiscipline())
                .capacity(clase.getCapacity())
                .price(clase.getPrice())
                .startTime(clase.getStartTime())
                .endTime(clase.getEndTime())
                .status(ClassStatus.PUBLISHED)
                .build();

        Class saved = classRepository.save(classEntity);

        if (clase.getStartTime() != null && clase.getEndTime() != null && maestroEmail != null) {
            LocalDateTime inicio = LocalDateTime.ofInstant(clase.getStartTime(), ZoneId.systemDefault());
            LocalDateTime fin = LocalDateTime.ofInstant(clase.getEndTime(), ZoneId.systemDefault());
            attributeService.asignarProfesorDependiente(
                    maestroEmail,
                    saved.getId(),
                    venue.getId(),
                    inicio,
                    fin);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Class> getCursosByMaestro(UUID maestroId) {
        return classRepository.findByTeacherId(maestroId);
    }

    @Transactional(readOnly = true)
    public List<Class> getCursosByComuna(String comuna) {
        List<Venue> venues = venueRepository.findAll().stream()
                .filter(v -> v.getAddress() != null && v.getAddress().toLowerCase().contains(comuna.toLowerCase()))
                .toList();

        return venues.stream()
                .flatMap(v -> roomRepository.findByVenueId(v.getId()).stream())
                .flatMap(r -> classRepository.findAll().stream()
                        .filter(c -> c.getRoom() != null && r.getId().equals(c.getRoom().getId())))
                .toList();
    }
}
