package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.domain.ClassStatusHistory;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.repository.ClassStatusHistoryRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.domain.ProfessionalProfile;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.repository.ProfessionalProfileRepository;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.RoomRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClassStatusHistoryRepository classStatusHistoryRepository;
    private final ProfessionalProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;

    public List<ClassResponse> listPublished() {
        return classRepository.findByStatusOrderByStartTimeAsc(ClassStatus.PUBLISHED).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public ClassResponse getById(UUID id) {
        Class c = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        return toResponse(c);
    }

    public List<ClassResponse> search(String disciplina, String comuna, String fechaDesde,
                                       String fechaHasta, Double precioMin, Double precioMax,
                                       String nivel) {
        Specification<Class> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ClassStatus.PUBLISHED));

            if (disciplina != null && !disciplina.isBlank()) {
                predicates.add(cb.equal(root.get("discipline"), Disciplina.valueOf(disciplina)));
            }
            if (nivel != null && !nivel.isBlank()) {
                predicates.add(cb.equal(root.get("level"), NivelClase.valueOf(nivel)));
            }
            if (fechaDesde != null && !fechaDesde.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), Instant.parse(fechaDesde)));
            }
            if (fechaHasta != null && !fechaHasta.isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), Instant.parse(fechaHasta)));
            }
            if (precioMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), precioMin));
            }
            if (precioMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), precioMax));
            }
            if (comuna != null && !comuna.isBlank()) {
                var venueJoin = root.join("room", JoinType.LEFT).join("venue", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(venueJoin.get("city")), comuna.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return classRepository.findAll(spec).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse create(ClassRequest req) {
        return createWithTeacher(req, null);
    }

    @Transactional
    public ClassResponse createWithTeacher(ClassRequest req, UUID teacherId) {
        return createClassInternal(req, teacherId, false);
    }

    @Transactional
    public ClassResponse createWithTeacher(ClassRequest req, UUID teacherId, boolean draft) {
        return createClassInternal(req, teacherId, draft);
    }

    /**
     * Crea un borrador de clase SIN sala asignada.
     * El rol TEACHER se asigna cuando se publica la clase (asignarReserva).
     */
    @Transactional
    public ClassResponse createBorrador(ClassRequest req, UUID teacherId) {
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
                .teacherId(teacherId)
                .tipoClase(TipoClase.PROPIA)
                .status(ClassStatus.DRAFT)
                .build();

        return toResponse(classRepository.save(c));
    }

    /**
     * Asigna una sala y horario a un borrador existente, luego lo publica.
     * Es en este paso donde se asigna el rol TEACHER (primera clase activa).
     */
    @Transactional
    public ClassResponse asignarReserva(UUID classId, UUID roomId, Instant startTime,
                                         Integer duration, UUID teacherId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        if (c.getTeacherId() != null && !c.getTeacherId().equals(teacherId)) {
            throw new BusinessException("No tienes permiso para modificar esta clase");
        }
        if (c.getStatus() != ClassStatus.DRAFT) {
            throw new BusinessException("Solo se puede asignar sala a clases en borrador");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (room.getVenue() == null || room.getVenue().getStatus() != EstadoSede.APROBADA) {
            throw new BusinessException("La sede de esta sala no esta aprobada.");
        }

        int dur = duration != null ? duration : (c.getDuration() != null ? c.getDuration() : 60);
        Instant endTime = startTime.plusSeconds(dur * 60L);

        List<Class> conflicts = classRepository.findConflictingClasses(roomId, startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new BusinessException("La sala ya esta reservada en ese horario. Elige otro horario disponible.");
        }

        c.setRoom(room);
        c.setStartTime(startTime);
        c.setEndTime(endTime);
        c.setDuration(dur);
        c.setStatus(ClassStatus.PUBLISHED);

        // Asignar rol TEACHER al publicar (BORRADOR sin sala → ACTIVA)
        boolean rolAsignadoEnReserva = false;
        if (teacherId != null) {
            rolAsignadoEnReserva = asignarRolTeacher(teacherId);
        }
        if (rolAsignadoEnReserva) {
            lastTeacherRoleAssigned.set(true);
        } else {
            lastTeacherRoleAssigned.remove();
        }

        return toResponse(classRepository.save(c));
    }

    @Transactional
    public ClassResponse completeClass(UUID classId, ClassRequest req, UUID teacherId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (c.getTeacherId() != null && !c.getTeacherId().equals(teacherId)) {
            throw new BusinessException("No tienes permiso para editar esta clase");
        }
        if (req.title() != null) c.setTitle(req.title());
        if (req.discipline() != null) c.setDiscipline(Disciplina.valueOf(req.discipline()));
        if (req.level() != null) c.setLevel(NivelClase.valueOf(req.level()));
        if (req.description() != null) c.setDescription(req.description());
        if (req.capacity() != null) c.setCapacity(req.capacity());
        if (req.duration() != null) {
            c.setDuration(req.duration());
            // Recalcular endTime cuando cambia la duracion (startTime queda fijo de la reserva)
            if (c.getStartTime() != null) {
                c.setEndTime(c.getStartTime().plusSeconds(req.duration() * 60L));
            }
        }
        if (req.price() != null) c.setPrice(req.price());
        if (req.minAge() != null) c.setMinAge(req.minAge());
        if (req.maxAge() != null) c.setMaxAge(req.maxAge());
        c.setStatus(ClassStatus.PUBLISHED);
        // Asignar rol TEACHER al publicar (BORRADOR con sala → ACTIVA)
        boolean rolAsignado = false;
        if (c.getTeacherId() != null) {
            rolAsignado = asignarRolTeacher(c.getTeacherId());
        }
        ClassResponse response = toResponse(classRepository.save(c));
        // Guardamos la señal para que el controller pueda incluirla en la respuesta
        if (rolAsignado) {
            lastTeacherRoleAssigned.set(true);
        } else {
            lastTeacherRoleAssigned.remove();
        }
        return response;
    }

    /**
     * ThreadLocal para señalizar si el rol TEACHER fue recién asignado durante la última operación.
     * El controller lo consulta para incluir atributosActualizados: true en la respuesta.
     */
    private static final ThreadLocal<Boolean> lastTeacherRoleAssigned = new ThreadLocal<>();

    public boolean wasTeacherRoleJustAssigned() {
        Boolean val = lastTeacherRoleAssigned.get();
        lastTeacherRoleAssigned.remove();
        return Boolean.TRUE.equals(val);
    }

    private ClassResponse createClassInternal(ClassRequest req, UUID teacherId, boolean draft) {
        Room room = roomRepository.findById(req.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getVenue() == null || room.getVenue().getStatus() != EstadoSede.APROBADA) {
            throw new BusinessException("La sede a la que pertenece esta sala no esta aprobada. Debe ser aprobada por el Administrador General antes de crear clases.");
        }

        Instant endTime = req.startTime().plusSeconds(req.duration() != null ? req.duration() * 60L : 3600L);
        List<Class> conflicts = classRepository.findConflictingClasses(req.roomId(), req.startTime(), endTime);
        if (!conflicts.isEmpty()) {
            throw new BusinessException("La sala ya esta reservada en ese horario. Elige otro horario disponible.");
        }

        if (teacherId != null) {
            IdentityVerification iv = identityVerificationRepository.findByUserId(teacherId).orElse(null);
            if (iv == null || !"APPROVED".equals(iv.getStatus())) {
                throw new BusinessException(
                    "Debes validar tu identidad antes de crear clases. Sube tu documento en tu perfil y espera la aprobacion.");
            }

            // Asignar TEACHER: al publicar directamente O al crear BORRADOR con sala (reserva de sala confirmada)
            if (!draft || req.roomId() != null) {
                boolean rolAsignado = asignarRolTeacher(teacherId);
                if (rolAsignado) lastTeacherRoleAssigned.set(true);
                else lastTeacherRoleAssigned.remove();
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
                .endTime(endTime)
                .room(room)
                .teacherId(teacherId)
                .tipoClase(teacherId != null ? TipoClase.PROPIA : TipoClase.ASIGNADA)
                .status(draft ? ClassStatus.DRAFT : ClassStatus.PUBLISHED)
                .build();

        return toResponse(classRepository.save(c));
    }

    public List<ClassResponse> getTeacherClasses(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClassResponse> getTeacherPropias(UUID teacherId) {
        return classRepository.findByTeacherIdAndTipoClase(teacherId, TipoClase.PROPIA).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClassResponse> getTeacherDrafts(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .filter(c -> c.getStatus() == ClassStatus.DRAFT)
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClassResponse> getTeacherAsignadas(UUID teacherId) {
        return classRepository.findByTeacherIdAndTipoClase(teacherId, TipoClase.ASIGNADA).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteDraft(UUID classId, UUID teacherId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));
        if (c.getTeacherId() == null || !c.getTeacherId().equals(teacherId)) {
            throw new BusinessException("No tienes permiso para eliminar esta clase");
        }
        if (c.getStatus() != ClassStatus.DRAFT) {
            throw new BusinessException("Solo puedes eliminar clases en borrador");
        }
        classRepository.deleteById(classId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherMetrics(UUID teacherId) {
        List<Class> clases = classRepository.findByTeacherId(teacherId);
        Instant primerDiaMes = YearMonth.now().atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long totalClases = clases.size();
        long clasesEsteMes = clases.stream()
                .filter(c -> c.getStartTime() != null && c.getStartTime().isAfter(primerDiaMes))
                .count();
        long totalAlumnos = clases.stream()
                .mapToLong(c -> enrollmentRepository.countByClassId(c.getId()))
                .sum();
        long clasesActivas = clases.stream()
                .filter(c -> c.getStatus() == ClassStatus.PUBLISHED || c.getStatus() == ClassStatus.IN_PROGRESS)
                .filter(c -> c.getStartTime() != null && c.getStartTime().isAfter(Instant.now()))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalClases", totalClases);
        result.put("totalAlumnos", totalAlumnos);
        result.put("clasesEsteMes", clasesEsteMes);
        result.put("clasesActivas", clasesActivas);
        result.put("asistenciaPromedio", 0);
        result.put("rating", 0);
        return result;
    }

    public List<ClassResponse> getByVenue(UUID venueId) {
        return classRepository.findByRoomVenueId(venueId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse updateStatus(UUID classId, ClassStatus status) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        ClassStatus previous = c.getStatus();
        c.setStatus(status);
        c = classRepository.save(c);

        ClassStatusHistory history = ClassStatusHistory.builder()
                .classEntity(c)
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(status.name())
                .changedBy(c.getTeacherId())
                .build();
        classStatusHistoryRepository.save(history);

        return toResponse(c);
    }

    /**
     * Asigna el rol TEACHER al usuario si aún no lo tiene.
     * Se llama una única vez: al publicar la primera clase activa del profesor.
     * @return true si el rol fue recién asignado (primera vez), false si ya lo tenía.
     */
    public boolean asignarRolTeacher(UUID teacherId) {
        Role teacherRole = roleRepository.findByName("TEACHER").orElse(null);
        if (teacherRole == null) return false;
        User user = userRepository.findById(teacherId).orElse(null);
        if (user == null) return false;
        boolean hasTeacherRole = user.getUserRoles().stream()
                .anyMatch(ur -> "TEACHER".equals(ur.getRole().getName()));
        if (!hasTeacherRole) {
            userRoleRepository.save(
                    new UserRole(new UserRoleId(user.getId(), teacherRole.getId()), user, teacherRole));
            notificationRepository.save(Notification.builder()
                    .userId(teacherId)
                    .type("CONTEXTO_PROFESOR_ACTIVADO")
                    .message("Tu contexto Maestro está activo. Ya puedes acceder al panel de profesor desde el menú superior.")
                    .read(false).createdAt(Instant.now()).build());
            return true;   // rol recién asignado
        }
        return false;
    }

    /**
     * Retorna el perfil público del profesor para una clase dada.
     * Usado en la vista de detalle de clase para alumnos.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherProfileForClass(UUID classId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        Map<String, Object> result = new HashMap<>();
        if (c.getTeacherId() == null) {
            return result;
        }

        User teacher = userRepository.findById(c.getTeacherId()).orElse(null);
        if (teacher != null) {
            result.put("id", teacher.getId());
            result.put("fullName", teacher.getFullName());
            result.put("socialName", teacher.getSocialName());
        }

        ProfessionalProfile profile = profileRepository.findByUser_Id(c.getTeacherId()).orElse(null);
        if (profile != null) {
            result.put("description", profile.getDescription());
            result.put("especialidad", profile.getEspecialidad());
            result.put("nivelEnsenanza", profile.getNivelEnsenanza());
            result.put("formacion", profile.getFormacion());
            result.put("experienceYears", profile.getExperienceYears());
            result.put("averageRating", profile.getAverageRating());
            result.put("photoUrl", profile.getPhotoUrl());
            result.put("instagram", profile.getInstagram());
            result.put("youtube", profile.getYoutube());
            result.put("sitioWeb", profile.getSitioWeb());
            result.put("linkedin", profile.getLinkedin());
        }

        return result;
    }

    private ClassResponse toResponse(Class c) {
        long enrolledCount = enrollmentRepository.countByClassId(c.getId());
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
                (int) enrolledCount, c.getCreatedAt());
    }
}
