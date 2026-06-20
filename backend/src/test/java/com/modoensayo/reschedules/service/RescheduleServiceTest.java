package com.modoensayo.reschedules.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.domain.RescheduleResponse;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import com.modoensayo.reschedules.enums.ResponseType;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.reschedules.repository.RescheduleRepository;
import com.modoensayo.reschedules.repository.RescheduleResponseRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.venues.repository.RoomScheduleBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RescheduleServiceTest {

    @Mock private RescheduleRepository rescheduleRepository;
    @Mock private RescheduleResponseRepository rescheduleResponseRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private RoomScheduleBlockRepository roomScheduleBlockRepository;

    @InjectMocks private RescheduleService rescheduleService;

    private UUID teacherId;
    private UUID venueAdminId;
    private UUID classId;
    private UUID rescheduleId;
    private Class propiaClass;
    private Class asignadaClass;
    private Reschedule reschedule;
    private Venue venue;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        venueAdminId = UUID.randomUUID();
        classId = UUID.randomUUID();
        rescheduleId = UUID.randomUUID();

        venue = Venue.builder().id(UUID.randomUUID()).adminId(venueAdminId).build();
        Room room = Room.builder().id(UUID.randomUUID()).venue(venue).build();

        propiaClass = Class.builder()
                .id(classId)
                .teacherId(teacherId)
                .tipoClase(TipoClase.PROPIA)
                .room(room)
                .build();

        asignadaClass = Class.builder()
                .id(classId)
                .teacherId(teacherId)
                .tipoClase(TipoClase.ASIGNADA)
                .room(room)
                .build();

        reschedule = Reschedule.builder()
                .id(rescheduleId)
                .classId(classId)
                .proposedTime(Instant.now().plusSeconds(3600))
                .status(RescheduleStatus.PROPOSED)
                .build();
    }

    // R19: Teacher can accept reschedule on PROPIA class
    @Test
    void teacherDecision_accept_shouldSucceed_forPropiaClassByOwner() {
        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(propiaClass));
        when(rescheduleRepository.save(any())).thenReturn(reschedule);
        when(classRepository.save(any())).thenReturn(propiaClass);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of());
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        assertDoesNotThrow(() ->
                rescheduleService.teacherDecision(rescheduleId, true, teacherId, false));

        verify(rescheduleRepository).save(argThat(r -> r.getStatus() == RescheduleStatus.TEACHER_ACCEPTED));
    }

    // R19: Wrong teacher cannot manage PROPIA class reschedule
    @Test
    void teacherDecision_shouldThrow_whenCallerIsNotOwnerOfPropiaClass() {
        UUID otherTeacher = UUID.randomUUID();
        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(propiaClass));

        assertThrows(BusinessException.class, () ->
                rescheduleService.teacherDecision(rescheduleId, true, otherTeacher, false));
    }

    // R19: Maestro Dependiente cannot manage ASIGNADA class reschedule
    @Test
    void teacherDecision_shouldThrow_whenDependentTeacherTriesAsignadaClass() {
        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(asignadaClass));

        // callerIsVenueAdmin=false → Maestro Dependiente blocked
        assertThrows(BusinessException.class, () ->
                rescheduleService.teacherDecision(rescheduleId, true, teacherId, false));
    }

    // R19: Venue admin CAN manage ASIGNADA class reschedule
    @Test
    void teacherDecision_shouldSucceed_whenVenueAdminManagesAsignadaClass() {
        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(asignadaClass));
        when(venueRepository.findByAdminId(venueAdminId)).thenReturn(List.of(venue));
        when(rescheduleRepository.save(any())).thenReturn(reschedule);
        when(classRepository.save(any())).thenReturn(asignadaClass);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of());
        doNothing().when(notificationService).enviar(any(), any(), any(), any());

        assertDoesNotThrow(() ->
                rescheduleService.teacherDecision(rescheduleId, true, venueAdminId, true));
    }

    // R19: Venue admin of wrong venue cannot manage
    @Test
    void teacherDecision_shouldThrow_whenVenueAdminDoesNotOwnVenue() {
        UUID otherAdmin = UUID.randomUUID();
        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(asignadaClass));
        when(venueRepository.findByAdminId(otherAdmin)).thenReturn(List.of()); // different venue

        assertThrows(BusinessException.class, () ->
                rescheduleService.teacherDecision(rescheduleId, true, otherAdmin, true));
    }

    // R9: Teacher rejects → all payments go to REFUND_PENDING
    @Test
    void teacherDecision_reject_shouldMarkPaymentsForRefund() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Enrollment enrollment = Enrollment.builder()
                .id(enrollmentId).classId(classId).beneficiaryId(studentId).build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID()).enrollment(enrollment)
                .amount(15000).status(PaymentStatus.RETAINED).build();

        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(classRepository.findById(classId)).thenReturn(Optional.of(propiaClass));
        when(rescheduleRepository.save(any())).thenReturn(reschedule);
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment));
        when(paymentRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);

        rescheduleService.teacherDecision(rescheduleId, false, teacherId, false);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUND_PENDING));
    }

    // R16: processTimeouts marks expired responses as TIMEOUT and refunds payments
    @Test
    void processTimeouts_shouldMarkTimeoutAndRefund_whenDeadlineExpired() {
        reschedule.setStatus(RescheduleStatus.TEACHER_ACCEPTED);
        reschedule.setResponseDeadline(Instant.now().minusSeconds(100));

        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        RescheduleResponse pendingResponse = RescheduleResponse.builder()
                .id(UUID.randomUUID()).rescheduleId(rescheduleId)
                .userId(studentId).responseType(null).build();
        Enrollment enrollment = Enrollment.builder()
                .id(enrollmentId).classId(classId).beneficiaryId(studentId).build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID()).enrollment(enrollment)
                .amount(15000).status(PaymentStatus.RETAINED).build();

        when(rescheduleRepository.findByStatusAndResponseDeadlineBefore(
                eq(RescheduleStatus.TEACHER_ACCEPTED), any())).thenReturn(List.of(reschedule));
        when(rescheduleResponseRepository.findByRescheduleIdAndResponseTypeIsNull(rescheduleId))
                .thenReturn(List.of(pendingResponse));
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment));
        when(paymentRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(rescheduleResponseRepository.save(any())).thenReturn(pendingResponse);
        when(rescheduleRepository.save(any())).thenReturn(reschedule);

        rescheduleService.processTimeouts();

        verify(rescheduleResponseRepository).save(
                argThat(r -> r.getResponseType() == ResponseType.TIMEOUT));
        verify(paymentRepository).save(
                argThat(p -> p.getStatus() == PaymentStatus.REFUND_PENDING));
        verify(rescheduleRepository).save(
                argThat(r -> r.getStatus() == RescheduleStatus.COMPLETED));
    }

    // Student decision: accept keeps payment RETAINED
    @Test
    void studentDecision_accept_shouldKeepPaymentRetained() {
        reschedule.setStatus(RescheduleStatus.TEACHER_ACCEPTED);
        UUID studentId = UUID.randomUUID();
        RescheduleResponse response = RescheduleResponse.builder()
                .id(UUID.randomUUID()).rescheduleId(rescheduleId)
                .userId(studentId).responseType(null).build();

        when(rescheduleRepository.findById(rescheduleId)).thenReturn(Optional.of(reschedule));
        when(rescheduleResponseRepository.findByRescheduleIdAndUserId(rescheduleId, studentId))
                .thenReturn(Optional.of(response));
        when(rescheduleResponseRepository.save(any())).thenReturn(response);
        doNothing().when(notificationService).enviar(any(), any(), any(), any());
        when(rescheduleResponseRepository.findByRescheduleIdAndResponseTypeIsNull(rescheduleId))
                .thenReturn(List.of());
        when(rescheduleRepository.save(any())).thenReturn(reschedule);

        rescheduleService.studentDecision(rescheduleId, true, studentId);

        verify(paymentRepository, never()).save(any()); // no payment change on accept
        verify(rescheduleResponseRepository).save(
                argThat(r -> r.getResponseType() == ResponseType.ACCEPTED));
    }
}
