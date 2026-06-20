package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.domain.ClassStatusHistory;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.repository.ClassStatusHistoryRepository;
import com.modoensayo.classes.repository.DisciplineCatalogRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.reschedules.service.NotificationService;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.domain.Role;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.repository.ProfessionalProfileRepository;
import com.modoensayo.users.repository.RoleRepository;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private IdentityVerificationRepository identityVerificationRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private ClassStatusHistoryRepository classStatusHistoryRepository;
    @Mock private ProfessionalProfileRepository profileRepository;
    @Mock private NotificationService notificationService;
    @Mock private DisciplineCatalogRepository disciplineCatalogRepository;

    @InjectMocks private ClassService classService;

    private UUID teacherId;
    private UUID classId;
    private UUID roomId;
    private User teacher;
    private IdentityVerification approvedIv;
    private Room room;
    private Venue venue;
    private Class draftClass;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        classId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        teacher = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .userRoles(new HashSet<>())
                .build();

        approvedIv = IdentityVerification.builder()
                .id(UUID.randomUUID())
                .userId(teacherId)
                .status("APPROVED")
                .build();

        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("Sede Test")
                .status(EstadoSede.APROBADA)
                .build();

        room = Room.builder()
                .id(roomId)
                .name("Sala A")
                .venue(venue)
                .build();

        draftClass = Class.builder()
                .id(classId)
                .title("Ballet Clásico")
                .discipline("Ballet")
                .status(ClassStatus.DRAFT)
                .teacherId(teacherId)
                .tipoClase(TipoClase.PROPIA)
                .build();
    }

    // --- createBorrador ---

    @Test
    void createBorrador_shouldCreateDraft_whenIdentityApproved() {
        when(identityVerificationRepository.findByUserId(teacherId)).thenReturn(Optional.of(approvedIv));
        when(classRepository.save(any(Class.class))).thenReturn(draftClass);
        when(enrollmentRepository.countByClassIdIn(any())).thenReturn(List.of());

        ClassRequest req = new ClassRequest(
                "Ballet Clásico", "Ballet", "BASICO", "Descripción", 10,
                60, 15000.0, null, null, null, null, null);

        ClassResponse result = classService.createBorrador(req, teacherId);

        assertNotNull(result);
        verify(classRepository).save(argThat(c -> c.getStatus() == ClassStatus.DRAFT));
    }

    @Test
    void createBorrador_shouldThrow_whenIdentityNotApproved() {
        IdentityVerification pending = IdentityVerification.builder()
                .userId(teacherId).status("PENDING").build();
        when(identityVerificationRepository.findByUserId(teacherId)).thenReturn(Optional.of(pending));

        ClassRequest req = new ClassRequest(
                "Ballet", "Ballet", "BASICO", "Desc", 10, 60, 15000.0,
                null, null, null, null, null);

        assertThrows(BusinessException.class, () -> classService.createBorrador(req, teacherId));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createBorrador_shouldThrow_whenNoIdentityRecord() {
        when(identityVerificationRepository.findByUserId(teacherId)).thenReturn(Optional.empty());

        ClassRequest req = new ClassRequest(
                "Ballet", "Ballet", "BASICO", "Desc", 10, 60, 15000.0,
                null, null, null, null, null);

        assertThrows(BusinessException.class, () -> classService.createBorrador(req, teacherId));
    }

    // --- asignarReserva ---

    @Test
    void asignarReserva_shouldPublishDraft_whenNoConflicts() {
        when(classRepository.findById(classId)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(classRepository.findConflictingClasses(eq(roomId), any(), any())).thenReturn(List.of());
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRoleRepository.findByUser_Id(teacherId)).thenReturn(List.of());
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(
                Role.builder().id(2).name("TEACHER").build()));
        when(userRoleRepository.save(any())).thenReturn(null);
        when(classRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.countByClassIdIn(any())).thenReturn(List.of());

        Instant start = Instant.now().plusSeconds(3600);
        ClassResponse result = classService.asignarReserva(classId, roomId, start, 60, teacherId);

        assertNotNull(result);
        verify(classRepository).save(argThat(c ->
                c.getStatus() == ClassStatus.PUBLISHED && c.getRoom() != null));
        assertEquals(ClassStatus.DRAFT, draftClass.getStatus());
    }

    @Test
    void asignarReserva_shouldThrow_whenRoomHasConflict() {
        Class conflicting = Class.builder().id(UUID.randomUUID()).build();
        when(classRepository.findById(classId)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(classRepository.findConflictingClasses(eq(roomId), any(), any())).thenReturn(List.of(conflicting));

        Instant start = Instant.now().plusSeconds(3600);
        assertThrows(BusinessException.class,
                () -> classService.asignarReserva(classId, roomId, start, 60, teacherId));
    }

    @Test
    void asignarReserva_shouldThrow_whenClassIsNotDraft() {
        draftClass.setStatus(ClassStatus.PUBLISHED);
        when(classRepository.findById(classId)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        Instant start = Instant.now().plusSeconds(3600);
        assertThrows(BusinessException.class,
                () -> classService.asignarReserva(classId, roomId, start, 60, teacherId));
    }

    @Test
    void asignarReserva_shouldThrow_whenVenueNotApproved() {
        venue.setStatus(EstadoSede.PENDIENTE_APROBACION);
        when(classRepository.findById(classId)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        Instant start = Instant.now().plusSeconds(3600);
        assertThrows(BusinessException.class,
                () -> classService.asignarReserva(classId, roomId, start, 60, teacherId));
    }

    // --- getById ---

    @Test
    void getById_shouldReturnClass_whenExists() {
        when(classRepository.findById(classId)).thenReturn(Optional.of(draftClass));
        when(enrollmentRepository.countByClassIdIn(any())).thenReturn(List.of());

        ClassResponse result = classService.getById(classId);
        assertNotNull(result);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(classRepository.findById(classId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> classService.getById(classId));
    }

    // --- listPublished ---

    @Test
    void listPublished_shouldReturnEmptyList_whenNoPublishedClasses() {
        when(classRepository.findPublishedWithRoomAndVenue(ClassStatus.PUBLISHED))
                .thenReturn(List.of());
        when(enrollmentRepository.countByClassIdIn(any())).thenReturn(List.of());

        List<ClassResponse> result = classService.listPublished();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listPublished_shouldReturnClasses_whenPublishedExist() {
        Class published = Class.builder()
                .id(UUID.randomUUID())
                .title("Salsa Avanzada")
                .status(ClassStatus.PUBLISHED)
                .discipline("Salsa")
                .build();
        when(classRepository.findPublishedWithRoomAndVenue(ClassStatus.PUBLISHED))
                .thenReturn(List.of(published));
        when(enrollmentRepository.countByClassIdIn(any())).thenReturn(List.of());

        List<ClassResponse> result = classService.listPublished();
        assertEquals(1, result.size());
    }
}
