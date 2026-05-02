package com.modoensayo.reschedules.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
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
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescheduleServiceDemoTest {

    @Mock private RescheduleRepository rescheduleRepository;
    @Mock private RescheduleResponseRepository responseRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RescheduleService rescheduleService;

    private UUID classId = UUID.randomUUID();
    private UUID teacherId = UUID.randomUUID();
    private UUID student1Id = UUID.randomUUID(); // accepts
    private UUID student2Id = UUID.randomUUID(); // accepts
    private UUID student3Id = UUID.randomUUID(); // rejects
    private UUID student4Id = UUID.randomUUID(); // timeout
    private Class classEntity;

    @BeforeEach
    void setUp() {
        classEntity = new Class();
        classEntity.setId(classId);
        classEntity.setTeacherId(teacherId);
        classEntity.setTitle("Clase de Ballet Sabado");
        classEntity.setStatus(ClassStatus.PUBLISHED);

        lenient().when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        lenient().when(classRepository.save(any(Class.class))).thenReturn(classEntity);
    }

    @Test
    void demo_saturdayScenario_fullFlow() {
        // === ESCENARIO: Sabado, clase de ballet se suspende ===
        Instant saturdayClass = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant newSundayTime = Instant.now().plus(25, ChronoUnit.HOURS);

        Reschedule reschedule = Reschedule.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .teacherId(teacherId)
                .proposedTime(newSundayTime)
                .reason("Profesor con problemas de salud")
                .status(RescheduleStatus.PROPOSED)
                .createdAt(Instant.now())
                .build();

        when(rescheduleRepository.save(any(Reschedule.class))).thenReturn(reschedule);
        when(rescheduleRepository.findById(reschedule.getId())).thenReturn(Optional.of(reschedule));

        // Step 1: Teacher proposes reschedule
        var result = rescheduleService.proposeReschedule(teacherId,
                new com.modoensayo.reschedules.dto.RescheduleRequest(classId, newSundayTime, "Profesor con problemas de salud"));

        assertEquals(RescheduleStatus.PROPOSED, result.status());
        assertEquals(ClassStatus.SUSPENDED, classEntity.getStatus());
        verify(classRepository).save(classEntity);

        // Step 2: Teacher ACCEPTS the proposal
        reschedule.setStatus(RescheduleStatus.TEACHER_ACCEPTED);
        // Mock 4 enrollments (3 students + 1 associate)
        List<Enrollment> enrollments = new ArrayList<>();
        for (UUID id : List.of(student1Id, student2Id, student3Id, student4Id)) {
            Enrollment e = new Enrollment();
            e.setId(UUID.randomUUID());
            e.setClassId(classId);
            e.setBeneficiaryId(id);
            e.setBeneficiaryType("USER");
            enrollments.add(e);
        }
        when(enrollmentRepository.findByClassId(classId)).thenReturn(enrollments);
        when(responseRepository.save(any(RescheduleResponse.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(rescheduleRepository.save(any(Reschedule.class))).thenReturn(reschedule);

        // Mock student responses
        RescheduleResponse r1 = RescheduleResponse.builder().id(UUID.randomUUID()).rescheduleId(reschedule.getId()).userId(student1Id).build();
        RescheduleResponse r2 = RescheduleResponse.builder().id(UUID.randomUUID()).rescheduleId(reschedule.getId()).userId(student2Id).build();
        RescheduleResponse r3 = RescheduleResponse.builder().id(UUID.randomUUID()).rescheduleId(reschedule.getId()).userId(student3Id).build();
        RescheduleResponse r4 = RescheduleResponse.builder().id(UUID.randomUUID()).rescheduleId(reschedule.getId()).userId(student4Id).build();
        when(responseRepository.findByRescheduleId(reschedule.getId()))
                .thenReturn(List.of(r1, r2, r3, r4));

        var result2 = rescheduleService.teacherDecision(teacherId,
                new com.modoensayo.reschedules.dto.TeacherDecisionRequest(reschedule.getId(), true));

        assertEquals(RescheduleStatus.TEACHER_ACCEPTED, result2.status());
        assertNotNull(result2.responseDeadline());
        verify(notificationRepository, times(4)).save(any(Notification.class)); // 4 students notified

        // Step 3: Student 1 ACCEPTS
        r1.setResponseType(ResponseType.ACCEPTED);
        r1.setRespondedAt(Instant.now());
        when(responseRepository.findByRescheduleIdAndUserId(reschedule.getId(), student1Id)).thenReturn(Optional.of(r1));
        when(responseRepository.findByRescheduleIdAndResponseTypeIsNull(reschedule.getId())).thenReturn(List.of(r2, r3, r4));

        rescheduleService.studentDecision(student1Id,
                new com.modoensayo.reschedules.dto.StudentDecisionRequest(reschedule.getId(), true));
        assertEquals(ResponseType.ACCEPTED, r1.getResponseType());

        // Student 2 ACCEPTS
        r2.setResponseType(ResponseType.ACCEPTED);
        r2.setRespondedAt(Instant.now());
        when(responseRepository.findByRescheduleIdAndUserId(reschedule.getId(), student2Id)).thenReturn(Optional.of(r2));

        rescheduleService.studentDecision(student2Id,
                new com.modoensayo.reschedules.dto.StudentDecisionRequest(reschedule.getId(), true));
        assertEquals(ResponseType.ACCEPTED, r2.getResponseType());

        // Student 3 REJECTS → refund
        r3.setResponseType(ResponseType.REJECTED);
        r3.setRespondedAt(Instant.now());
        when(responseRepository.findByRescheduleIdAndUserId(reschedule.getId(), student3Id)).thenReturn(Optional.of(r3));
        when(responseRepository.findByRescheduleIdAndResponseTypeIsNull(reschedule.getId())).thenReturn(List.of(r4));

        // Mock enrollment for student3 refund
        Enrollment e3 = new Enrollment();
        e3.setId(UUID.randomUUID());
        e3.setBeneficiaryId(student3Id);
        Payment p3 = Payment.builder().id(UUID.randomUUID()).amount(5000).status(PaymentStatus.RETAINED).build();
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(e3));
        when(paymentRepository.findByEnrollmentId(e3.getId())).thenReturn(List.of(p3));

        rescheduleService.studentDecision(student3Id,
                new com.modoensayo.reschedules.dto.StudentDecisionRequest(reschedule.getId(), false));
        assertEquals(ResponseType.REJECTED, r3.getResponseType());
        assertEquals(PaymentStatus.REFUND_PENDING, p3.getStatus()); // payment marked for refund

        // Step 4: Student 4 TIMEOUT (48h passed, processed by scheduled task)
        reschedule.setResponseDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        when(rescheduleRepository.findByStatusAndResponseDeadlineBefore(eq(RescheduleStatus.TEACHER_ACCEPTED), any(Instant.class)))
                .thenReturn(List.of(reschedule));
        when(responseRepository.findByRescheduleIdAndResponseTypeIsNull(reschedule.getId())).thenReturn(List.of(r4));
        r4.setResponseType(ResponseType.TIMEOUT);

        // Mock enrollment for student4 timeout refund
        Enrollment e4 = new Enrollment();
        e4.setId(UUID.randomUUID());
        e4.setBeneficiaryId(student4Id);
        Payment p4 = Payment.builder().id(UUID.randomUUID()).amount(5000).status(PaymentStatus.RETAINED).build();
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(e4));
        when(paymentRepository.findByEnrollmentId(e4.getId())).thenReturn(List.of(p4));

        rescheduleService.processTimeouts();

        assertEquals(ResponseType.TIMEOUT, r4.getResponseType());
        assertEquals(PaymentStatus.REFUND_PENDING, p4.getStatus()); // timeout → refund

        // Final state:
        // - 2 students accepted (payments stay RETAINED)
        // - 1 student rejected (REFUND_PENDING)
        // - 1 student timeout (REFUND_PENDING)
        // - Reschedule COMPLETED
        verify(responseRepository, atLeast(1)).findByRescheduleIdAndResponseTypeIsNull(reschedule.getId());
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    @Test
    void demo_teacherRejects_allPaymentsToRefund() {
        Reschedule reschedule = Reschedule.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .teacherId(teacherId)
                .proposedTime(Instant.now().plus(24, ChronoUnit.HOURS))
                .status(RescheduleStatus.PROPOSED)
                .build();

        when(rescheduleRepository.findById(reschedule.getId())).thenReturn(Optional.of(reschedule));

        // Mock enrollments
        List<Enrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Enrollment e = new Enrollment();
            e.setId(UUID.randomUUID());
            e.setClassId(classId);
            e.setBeneficiaryId(UUID.randomUUID());
            enrollments.add(e);
        }
        when(enrollmentRepository.findByClassId(classId)).thenReturn(enrollments);

        // Mock payments for each enrollment
        for (Enrollment e : enrollments) {
            Payment p = Payment.builder().id(UUID.randomUUID()).amount(5000).status(PaymentStatus.RETAINED).build();
            when(paymentRepository.findByEnrollmentId(e.getId())).thenReturn(List.of(p));
        }

        rescheduleService.teacherDecision(teacherId,
                new com.modoensayo.reschedules.dto.TeacherDecisionRequest(reschedule.getId(), false));

        assertEquals(RescheduleStatus.TEACHER_REJECTED, reschedule.getStatus());
        verify(paymentRepository, times(3)).save(any(Payment.class)); // 3 payments → REFUND_PENDING
    }
}
