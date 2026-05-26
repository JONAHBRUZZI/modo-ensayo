package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.enums.EstadoSede;
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
    private final IdentityVerificationRepository identityVerificationRepository;

    public List<ClassResponse> listPublished() {
        return classRepository.findByStatusOrderByStartTimeAsc(ClassStatus.PUBLISHED).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse create(ClassRequest req) {
        return createWithTeacher(req, null);
    }

    @Transactional
    public ClassResponse createWithTeacher(ClassRequest req, UUID teacherId) {
        Room room = roomRepository.findById(req.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getVenue() == null || room.getVenue().getStatus() != EstadoSede.APROBADA) {
            throw new BusinessException("La sede a la que pertenece esta sala no esta aprobada. Debe ser aprobada por el Administrador General antes de crear clases.");
        }

        if (teacherId != null) {
            IdentityVerification iv = identityVerificationRepository.findByUserId(teacherId).orElse(null);
            if (iv == null || !"APPROVED".equals(iv.getStatus())) {
                throw new BusinessException(
                    "Debes validar tu identidad antes de crear clases. Sube tu documento en tu perfil y espera la aprobacion.");
            }
        }

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
                .teacherId(teacherId)
                .tipoClase(teacherId != null ? TipoClase.PROPIA : TipoClase.ASIGNADA)
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
                c.getTipoClase() != null ? c.getTipoClase().name() : null,
                0, c.getCreatedAt());
    }
}
