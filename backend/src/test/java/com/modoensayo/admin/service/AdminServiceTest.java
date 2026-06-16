package com.modoensayo.admin.service;

import com.modoensayo.attendance.repository.AttendanceRepository;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.payments.repository.PaymentSessionRepository;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.reschedules.service.NotificationService;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.repository.*;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private IdentityVerificationRepository identityVerificationRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentSessionRepository paymentSessionRepository;
    @Mock private AttendanceRepository attendanceRepository;

    @InjectMocks private AdminService adminService;

    private UUID adminId;
    private UUID userId;
    private UUID ivId;
    private UUID venueId;
    private User user;
    private IdentityVerification iv;
    private Venue venue;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        ivId = UUID.randomUUID();
        venueId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("user@test.com")
                .fullName("Test User")
                .enabled(true)
                .userRoles(new HashSet<>())
                .build();

        iv = IdentityVerification.builder()
                .id(ivId)
                .userId(userId)
                .documentUrl("https://example.com/doc.pdf")
                .status("PENDING")
                .build();

        venue = Venue.builder()
                .id(venueId)
                .name("Academia Test")
                .status(EstadoSede.PENDIENTE_APROBACION)
                .adminId(userId)
                .build();
    }

    // --- reviewIdentity ---

    @Test
    void reviewIdentity_shouldApprove_andUpdateUser() {
        when(identityVerificationRepository.findById(ivId)).thenReturn(Optional.of(iv));
        when(identityVerificationRepository.save(any())).thenReturn(iv);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        IdentityVerificationResponse result =
                adminService.reviewIdentity(ivId, "approve", adminId);

        assertNotNull(result);
        verify(identityVerificationRepository).save(argThat(v -> "APPROVED".equals(v.getStatus())));
        verify(userRepository).save(argThat(u -> u.isIdentidadValidada() && "APROBADO".equals(u.getIdentidadEstado())));
        // NO debe asignar rol TEACHER (bug R20 corregido)
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void reviewIdentity_shouldReject_andUpdateUser() {
        when(identityVerificationRepository.findById(ivId)).thenReturn(Optional.of(iv));
        when(identityVerificationRepository.save(any())).thenReturn(iv);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        IdentityVerificationResponse result =
                adminService.reviewIdentity(ivId, "reject", adminId);

        assertNotNull(result);
        verify(identityVerificationRepository).save(argThat(v -> "REJECTED".equals(v.getStatus())));
        verify(userRepository).save(argThat(u -> !u.isIdentidadValidada() && "RECHAZADO".equals(u.getIdentidadEstado())));
    }

    @Test
    void reviewIdentity_shouldThrow_whenIvNotFound() {
        when(identityVerificationRepository.findById(ivId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> adminService.reviewIdentity(ivId, "approve", adminId));
    }

    // --- approveVenue ---

    @Test
    void approveVenue_shouldApproveAndAssignVenueAdminRole() {
        Role venueAdminRole = Role.builder().id(3).name("VENUE_ADMIN").build();

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueRepository.save(any())).thenReturn(venue);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("VENUE_ADMIN")).thenReturn(Optional.of(venueAdminRole));
        when(userRoleRepository.save(any())).thenReturn(null);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        var result = adminService.approveVenue(venueId);

        assertNotNull(result);
        verify(venueRepository).save(argThat(v -> v.getStatus() == EstadoSede.APROBADA));
        verify(userRoleRepository).save(argThat(ur -> "VENUE_ADMIN".equals(ur.getRole().getName())));
    }

    @Test
    void approveVenue_shouldNotDuplicateVenueAdminRole_whenAlreadyHasIt() {
        Role venueAdminRole = Role.builder().id(3).name("VENUE_ADMIN").build();
        UserRole existingRole = UserRole.builder().user(user).role(venueAdminRole).build();
        user.getUserRoles().add(existingRole);

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueRepository.save(any())).thenReturn(venue);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("VENUE_ADMIN")).thenReturn(Optional.of(venueAdminRole));
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        adminService.approveVenue(venueId);

        // El rol ya existe → no debe guardarse de nuevo
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void approveVenue_shouldThrow_whenVenueNotFound() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.approveVenue(venueId));
    }

    // --- toggleUser (suspender/reactivar) ---

    @Test
    void toggleUser_shouldSuspendUser_withMotivo() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        assertDoesNotThrow(() -> adminService.toggleUser(userId, "Incumplimiento de normas"));

        verify(userRepository).save(argThat(u -> !u.isEnabled()));
        verify(notificationService).enviar(eq(userId), any(), any(), contains("SUSPENDIDA"));
    }

    @Test
    void toggleUser_shouldThrow_whenSuspendingWithoutMotivo() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> adminService.toggleUser(userId, ""));
        assertThrows(BusinessException.class, () -> adminService.toggleUser(userId, null));
    }

    @Test
    void toggleUser_shouldReactivateUser_withoutMotivo() {
        user.setEnabled(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        assertDoesNotThrow(() -> adminService.toggleUser(userId, null));

        verify(userRepository).save(argThat(User::isEnabled));
        verify(notificationService).enviar(eq(userId), any(), any(), contains("REACTIVADA"));
    }

    @Test
    void toggleUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> adminService.toggleUser(userId, "motivo"));
    }

    // --- assignRole / revokeRole ---

    @Test
    void assignRole_shouldSaveNewRole_whenUserDoesNotHaveIt() {
        Role teacherRole = Role.builder().id(2).name("TEACHER").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRoleRepository.save(any())).thenReturn(null);

        assertDoesNotThrow(() -> adminService.assignRole(userId, "TEACHER"));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void assignRole_shouldSkip_whenUserAlreadyHasRole() {
        Role teacherRole = Role.builder().id(2).name("TEACHER").build();
        UserRole existing = UserRole.builder().user(user).role(teacherRole).build();
        user.getUserRoles().add(existing);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));

        adminService.assignRole(userId, "TEACHER");
        verify(userRoleRepository, never()).save(any());
    }
}
