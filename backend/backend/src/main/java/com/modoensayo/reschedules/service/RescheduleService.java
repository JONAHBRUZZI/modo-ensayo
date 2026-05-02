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
import com.modoensayo.reschedules.dto.NotificationDto;
import com.modoensayo.reschedules.dto.RescheduleRequest;
import com.modoensayo.reschedules.dto.RescheduleResponseDto;
import com.modoensayo.reschedules.dto.StudentDecisionRequest;
import com.modoensayo.reschedules.dto.StudentResponseDto;
import com.modoensayo.reschedules.dto.TeacherDecisionRequest;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import com.modoensayo.reschedules.enums.ResponseType;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.reschedules.repository.RescheduleRepository;
import com.modoensayo.reschedules.repository.RescheduleResponseRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final RescheduleRepository rescheduleRepository;
    private final RescheduleResponseRepository responseRepository;
    private final NotificationRepository notificationRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Step 1: Teacher proposes reschedule. System suggests date.
     * Class goes to SUSPENDED. Reschedule created with status PROPOSED.
     */
    @Transactional
    public RescheduleResponseDto proposeReschedule(UUID teacherId, RescheduleRequest request) {
        Class classEntity = classRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!teacherId.equals(classEntity.getTeacherId())) {
            throw new BusinessException("Only the class teacher can propose a reschedule");
        }

        // Suspend the class
        classEntity.setStatus(ClassStatus.SUSPENDED);
        classRepository.save(classEntity);

        // Create reschedule proposal
        Reschedule reschedule = Reschedule.builder()
                .classId(request.classId())
                .teacherId(teacherId)
                .proposedTime(request.proposedTime())
                .reason(request.reason())
                .status(RescheduleStatus.PROPOSED)
                .createdAt(Instant.now())
                .build();
        reschedule = rescheduleRepository.save(reschedule);

        log.info("Reschedule PROPOSED: class={}, newTime={}, teacher={}",
                request.classId(), request.proposedTime(), teacherId);

        return toDto(reschedule);
    }

    /**
     * Step 2: Teacher accepts the proposed reschedule.
     * Sets 48h deadline, notifies all enrolled students.
     */
    @Transactional
    public RescheduleResponseDto teacherDecision(UUID teacherId, TeacherDecisionRequest request) {
        Reschedule reschedule = rescheduleRepository.findById(request.rescheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));

        if (!teacherId.equals(reschedule.getTeacherId())) {
            throw new BusinessException("Only the class teacher can decide");
        }

        if (!request.accepted()) {
            // Teacher rejects → all payments go to REFUND_PENDING
            reschedule.setStatus(RescheduleStatus.TEACHER_REJECTED);
            rescheduleRepository.save(reschedule);

            refundAllEnrollments(reschedule.getClassId(), "Clase cancelada: profesor rechazo reagendamiento");
            log.info("Teacher REJECTED reschedule: class={}, all payments->REFUND_PENDING", reschedule.getClassId());
            return toDto(reschedule);
        }

        // Teacher accepts: create new class, set 48h deadline, notify students
        Class originalClass = classRepository.findById(reschedule.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Original class not found"));

        long durationSeconds = originalClass.getEndTime() != null && originalClass.getStartTime() != null
                ? originalClass.getEndTime().getEpochSecond() - originalClass.getStartTime().getEpochSecond()
                : 3600;
        Instant endTime = reschedule.getProposedTime().plus(durationSeconds, java.time.temporal.ChronoUnit.SECONDS);

        Class newClass = Class.builder()
                .room(originalClass.getRoom())
                .teacherId(originalClass.getTeacherId())
                .title(originalClass.getTitle())
                .discipline(originalClass.getDiscipline())
                .capacity(originalClass.getCapacity())
                .price(originalClass.getPrice())
                .startTime(reschedule.getProposedTime())
                .endTime(endTime)
                .status(ClassStatus.PUBLISHED)
                .build();
        classRepository.save(newClass);

        reschedule.setStatus(RescheduleStatus.TEACHER_ACCEPTED);
        reschedule.setNewClassId(newClass.getId());
        Instant deadline = Instant.now().plus(48, ChronoUnit.HOURS);
        reschedule.setResponseDeadline(deadline);
        rescheduleRepository.save(reschedule);

        // Create student responses (pending) and notify them
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(reschedule.getClassId());
        for (Enrollment enrollment : enrollments) {
            UUID studentId = enrollment.getBeneficiaryId();
            if (studentId == null) continue;

            // Create pending response record
            RescheduleResponse response = RescheduleResponse.builder()
                    .rescheduleId(reschedule.getId())
                    .userId(studentId)
                    .build();
            responseRepository.save(response);

            // Notify student
            String className = originalClass.getTitle();
            notifyUser(studentId, String.format(
                    "La clase '%s' fue reagendada para %s. Tienes 48h para confirmar tu asistencia.",
                    className, reschedule.getProposedTime()));
        }

        log.info("Teacher ACCEPTED reschedule: class={}, newClass={}, deadline={}, students notified",
                reschedule.getClassId(), newClass.getId(), deadline);
        return toDto(reschedule);
    }

    /**
     * Step 3: Student individually accepts or rejects.
     */
    @Transactional
    public RescheduleResponseDto studentDecision(UUID userId, StudentDecisionRequest request) {
        Reschedule reschedule = rescheduleRepository.findById(request.rescheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));

        if (reschedule.getStatus() != RescheduleStatus.TEACHER_ACCEPTED) {
            throw new BusinessException("Reschedule is not in a state that allows student decisions");
        }

        if (reschedule.getResponseDeadline() != null
                && Instant.now().isAfter(reschedule.getResponseDeadline())) {
            throw new BusinessException("Response deadline has passed");
        }

        RescheduleResponse response = responseRepository
                .findByRescheduleIdAndUserId(request.rescheduleId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending response found for this user"));

        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getFullName() : userId.toString();

        if (request.accepted()) {
            response.setResponseType(ResponseType.ACCEPTED);
            response.setRespondedAt(Instant.now());
            responseRepository.save(response);

            // Move enrollment to new class (payment stays RETAINED)
            Enrollment oldEnrollment = enrollmentRepository.findByClassId(reschedule.getClassId())
                    .stream()
                    .filter(e -> beneficiaryIdEquals(e, userId))
                    .findFirst()
                    .orElse(null);

            if (oldEnrollment != null && reschedule.getNewClassId() != null) {
                Enrollment newEnrollment = Enrollment.builder()
                        .classId(reschedule.getNewClassId())
                        .beneficiaryType(oldEnrollment.getBeneficiaryType())
                        .beneficiaryId(oldEnrollment.getBeneficiaryId())
                        .status("ACTIVE")
                        .build();
                enrollmentRepository.save(newEnrollment);

                // Copy payment to new enrollment (stays RETAINED)
                List<Payment> oldPayments = paymentRepository.findByEnrollmentId(oldEnrollment.getId());
                for (Payment oldPayment : oldPayments) {
                    if (oldPayment.getStatus() == PaymentStatus.RETAINED) {
                        Payment newPayment = Payment.builder()
                                .enrollment(newEnrollment)
                                .amount(oldPayment.getAmount())
                                .status(PaymentStatus.RETAINED)
                                .build();
                        paymentRepository.save(newPayment);
                    }
                }
                log.info("Enrollment moved to new class {} for user {}", reschedule.getNewClassId(), userName);
            }
        } else {
            response.setResponseType(ResponseType.REJECTED);
            response.setRespondedAt(Instant.now());
            responseRepository.save(response);

            // Refund this student's payment
            refundStudentEnrollment(reschedule.getClassId(), userId,
                    "Clase reagendada: alumno rechazo nueva fecha");
            log.info("Student '{}' REJECTED reschedule: class={}, payment->REFUND_PENDING", userName, reschedule.getClassId());
        }

        checkRescheduleCompleted(reschedule);
        return toDto(reschedule);
    }

    /**
     * Process timeouts: called by @Scheduled task every hour.
     * Students who haven't responded within 48h → TIMEOUT → refund.
     */
    @Transactional
    public void processTimeouts() {
        Instant now = Instant.now();
        List<Reschedule> expired = rescheduleRepository
                .findByStatusAndResponseDeadlineBefore(RescheduleStatus.TEACHER_ACCEPTED, now);

        for (Reschedule reschedule : expired) {
            List<RescheduleResponse> pending = responseRepository
                    .findByRescheduleIdAndResponseTypeIsNull(reschedule.getId());

            for (RescheduleResponse response : pending) {
                response.setResponseType(ResponseType.TIMEOUT);
                response.setRespondedAt(now);
                responseRepository.save(response);

                refundStudentEnrollment(reschedule.getClassId(), response.getUserId(),
                        "Clase reagendada: no respondio en 48h (timeout)");

                User user = userRepository.findById(response.getUserId()).orElse(null);
                String userName = user != null ? user.getFullName() : response.getUserId().toString();
                log.info("TIMEOUT: student '{}' did not respond within 48h for class={}", userName, reschedule.getClassId());
            }

            checkRescheduleCompleted(reschedule);
        }

        if (!expired.isEmpty()) {
            log.info("Processed {} reschedule timeouts", expired.size());
        }
    }

    /**
     * Check if all students have responded. If so, mark reschedule as COMPLETED.
     */
    private void checkRescheduleCompleted(Reschedule reschedule) {
        long pending = responseRepository.findByRescheduleIdAndResponseTypeIsNull(reschedule.getId()).size();
        if (pending == 0) {
            reschedule.setStatus(RescheduleStatus.COMPLETED);
            rescheduleRepository.save(reschedule);
            log.info("Reschedule COMPLETED: class={}", reschedule.getClassId());
        }
    }

    /**
     * Refund ALL enrollments for a class (teacher rejects).
     */
    private void refundAllEnrollments(UUID classId, String reason) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        for (Enrollment enrollment : enrollments) {
            List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.RETAINED) {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    paymentRepository.save(payment);
                }
            }
            if (enrollment.getBeneficiaryId() != null) {
                notifyUser(enrollment.getBeneficiaryId(), reason);
            }
        }
    }

    /**
     * Refund a single student's enrollment in a class.
     */
    private void refundStudentEnrollment(UUID classId, UUID beneficiaryId, String reason) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        for (Enrollment enrollment : enrollments) {
            if (beneficiaryId.equals(enrollment.getBeneficiaryId())) {
                List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
                for (Payment payment : payments) {
                    if (payment.getStatus() == PaymentStatus.RETAINED) {
                        payment.setStatus(PaymentStatus.REFUND_PENDING);
                        paymentRepository.save(payment);
                    }
                }
            }
        }
        notifyUser(beneficiaryId, reason);
    }

    /**
     * Create in-app notification for a user.
     */
    private void notifyUser(UUID userId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
    }

    // --- Query methods ---

    @Transactional(readOnly = true)
    public RescheduleResponseDto getReschedule(UUID rescheduleId) {
        Reschedule reschedule = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));
        return toDto(reschedule);
    }

    @Transactional(readOnly = true)
    public List<RescheduleResponseDto> getReschedulesByClass(UUID classId) {
        return rescheduleRepository.findByClassId(classId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RescheduleResponseDto> getReschedulesByTeacher(UUID teacherId) {
        return rescheduleRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDto(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markNotificationRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllNotificationsRead(UUID userId) {
        notificationRepository.updateAllReadByUserId(userId);
    }

    // --- DTO mapping ---

    private RescheduleResponseDto toDto(Reschedule r) {
        List<RescheduleResponse> responses = responseRepository.findByRescheduleId(r.getId());
        Class classEntity = classRepository.findById(r.getClassId()).orElse(null);
        String classTitle = classEntity != null ? classEntity.getTitle() : r.getClassId().toString();

        List<StudentResponseDto> studentDtos = new ArrayList<>();
        for (RescheduleResponse resp : responses) {
            User user = userRepository.findById(resp.getUserId()).orElse(null);
            studentDtos.add(new StudentResponseDto(
                    resp.getId(),
                    resp.getUserId(),
                    user != null ? user.getEmail() : null,
                    user != null ? user.getFullName() : null,
                    resp.getResponseType(),
                    resp.getRespondedAt()
            ));
        }

        return new RescheduleResponseDto(
                r.getId(),
                r.getClassId(),
                r.getNewClassId(),
                classTitle,
                r.getTeacherId(),
                r.getProposedTime(),
                r.getReason(),
                r.getStatus(),
                r.getResponseDeadline(),
                r.getCreatedAt(),
                studentDtos
        );
    }

    private boolean beneficiaryIdEquals(Enrollment enrollment, UUID userId) {
        return userId.equals(enrollment.getBeneficiaryId());
    }
}
