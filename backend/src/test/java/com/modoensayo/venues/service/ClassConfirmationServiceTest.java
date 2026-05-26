package com.modoensayo.venues.service;

import com.modoensayo.attendance.repository.AttendanceRepository;
import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.domain.ClassStatusHistory;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.repository.ClassStatusHistoryRepository;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassConfirmationServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private ClassStatusHistoryRepository classStatusHistoryRepository;

    @InjectMocks private ClassConfirmationService service;

    private UUID adminId;
    private UUID classId;
    private UUID enrollmentId;
    private Class classEntity;
    private Venue venue;
    private Enrollment enrollment;
    private Payment payment;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        classId = UUID.randomUUID();
        enrollmentId = UUID.randomUUID();

        venue = Venue.builder().id(UUID.randomUUID()).adminId(adminId).name("Sede Test").build();

        Room room = Room.builder().id(UUID.randomUUID()).name("Sala 1").venue(venue).build();

        classEntity = Class.builder()
                .id(classId)
                .title("Ballet Folclórico")
                .status(ClassStatus.POR_VALIDAR)
                .teacherId(UUID.randomUUID())
                .tipoClase(TipoClase.PROPIA)
                .room(room)
                .build();

        enrollment = Enrollment.builder()
                .id(enrollmentId)
                .classId(classId)
                .beneficiaryId(UUID.randomUUID())
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .amount(15000)
                .status(PaymentStatus.RETAINED)
                .build();
    }

    // R8: Confirmar clase REALIZADA libera los pagos
    @Test
    void confirmRealized_shouldReleasePayments_whenClassIsValid() {
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(classRepository.save(any())).thenReturn(classEntity);
        when(classStatusHistoryRepository.save(any(ClassStatusHistory.class))).thenReturn(null);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment));
        when(paymentRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(notificationRepository.save(any(Notification.class))).thenReturn(null);

        ClassConfirmationService.ClassConfirmationResult result =
                service.confirmClassRealized(adminId.toString(), classId.toString());

        assertEquals("COMPLETED", result.newStatus());
        assertEquals(1, result.affectedPayments());
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.RELEASED));
    }

    // R9: No realizada con clase PROPIA → notifica al Maestro Independiente
    @Test
    void confirmNotRealized_shouldNotifyTeacher_whenClassIsPropia() {
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(classRepository.save(any())).thenReturn(classEntity);
        when(classStatusHistoryRepository.save(any())).thenReturn(null);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment));
        when(paymentRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(notificationRepository.save(any())).thenReturn(null);

        service.confirmClassNotRealized(adminId.toString(), classId.toString());

        // 1 notification to teacher + 1 to beneficiary
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUND_PENDING));
    }

    // R19: No realizada con clase ASIGNADA → notifica al Admin Sede y al Maestro Dependiente
    @Test
    void confirmNotRealized_shouldNotifyVenueAdmin_whenClassIsAsignada() {
        classEntity.setTipoClase(TipoClase.ASIGNADA);

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(classRepository.save(any())).thenReturn(classEntity);
        when(classStatusHistoryRepository.save(any())).thenReturn(null);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment));
        when(paymentRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(notificationRepository.save(any())).thenReturn(null);

        service.confirmClassNotRealized(adminId.toString(), classId.toString());

        // Should notify venue admin + teacher (at least 2 notifications)
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    void confirmRealized_shouldThrow_whenClassNotInPorValidar() {
        classEntity.setStatus(ClassStatus.COMPLETED);
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

        assertThrows(BusinessException.class,
                () -> service.confirmClassRealized(adminId.toString(), classId.toString()));
    }

    @Test
    void confirmRealized_shouldThrow_whenAdminDoesNotOwnVenue() {
        UUID otherAdmin = UUID.randomUUID();
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

        assertThrows(BusinessException.class,
                () -> service.confirmClassRealized(otherAdmin.toString(), classId.toString()));
    }
}
