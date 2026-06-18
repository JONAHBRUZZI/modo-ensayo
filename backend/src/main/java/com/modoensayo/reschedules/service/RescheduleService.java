package com.modoensayo.reschedules.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.domain.RescheduleResponse;
import com.modoensayo.reschedules.dto.*;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import com.modoensayo.reschedules.enums.ResponseType;
import com.modoensayo.reschedules.repository.*;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.dto.RoomAvailabilityResponse;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.venues.service.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final RescheduleRepository rescheduleRepository;
    private final RescheduleResponseRepository rescheduleResponseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final ClassRepository classRepository;
    private final VenueRepository venueRepository;
    private final RoomAvailabilityService roomAvailabilityService;

    @Transactional
    public RescheduleResponseDto propose(RescheduleRequest req) {
        Class classEntity = classRepository.findById(req.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // TODO: RoomAvailability removed. Validation bypassed temporarily.
        // Re-enable slot validation when VenueScheduleService is implemented.
        List<RoomAvailabilityResponse> availableSlots = roomAvailabilityService
                .getAvailableSlotsForReschedule(
                    classEntity.getRoom().getVenue().getId().toString(),
                    Instant.now());

        boolean isValid = availableSlots.isEmpty() || availableSlots.stream().anyMatch(slot -> {
            Instant start = slot.startTime();
            Instant end = slot.endTime();
            return !req.proposedTime().isBefore(start) && !req.proposedTime().isAfter(end);
        });

        if (!isValid) {
            throw new BusinessException(
                "La fecha propuesta no esta dentro de los bloques horarios disponibles de la sede. " +
                "Consulta las opciones disponibles en /reschedules/class/{classId}/available-slots.");
        }

        Reschedule r = Reschedule.builder()
                .classId(req.classId()).proposedTime(req.proposedTime()).reason(req.reason())
                .status(RescheduleStatus.PROPOSED)
                .build();
        return toDto(rescheduleRepository.save(r));
    }

    /**
     * R19: For PROPIA classes only the teacher (owner) can decide.
     * For ASIGNADA classes only the venue admin can decide.
     * callerIsVenueAdmin=true when the caller has ADMIN_SEDE role.
     */
    @Transactional
    public RescheduleResponseDto teacherDecision(UUID rescheduleId, boolean accepted, UUID callerId, boolean callerIsVenueAdmin) {
        Reschedule r = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));

        if (r.getStatus() != RescheduleStatus.PROPOSED) {
            throw new BusinessException("Este reagendamiento no esta en estado PROPOSED.");
        }

        // R19: validate authority based on class type
        Class classEntity = classRepository.findById(r.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        TipoClase tipo = classEntity.getTipoClase() != null ? classEntity.getTipoClase() : TipoClase.PROPIA;

        if (tipo == TipoClase.PROPIA) {
            if (!callerId.equals(classEntity.getTeacherId())) {
                throw new BusinessException("Solo el Maestro Independiente dueno de esta clase puede gestionar su reagendamiento (R19).");
            }
        } else {
            // ASIGNADA: only the venue admin of this venue can act
            if (!callerIsVenueAdmin) {
                throw new BusinessException("El Maestro Dependiente no puede gestionar el reagendamiento de una Clase Asignada. Corresponde al Administrador de Sede (R19).");
            }
            boolean adminOwnsVenue = venueRepository.findByAdminId(callerId).stream()
                    .anyMatch(v -> v.getId().equals(classEntity.getRoom().getVenue().getId()));
            if (!adminOwnsVenue) {
                throw new BusinessException("Solo el Administrador de Sede de esta clase puede gestionar su reagendamiento (R19).");
            }
        }

        UUID teacherId = callerId;

        if (accepted) {
            r.setStatus(RescheduleStatus.TEACHER_ACCEPTED);
            r.setResponseDeadline(Instant.now().plusSeconds(48 * 3600));
            r.setTeacherId(teacherId);
            rescheduleRepository.save(r);

            if (r.getProposedTime() != null) {
                classEntity.setStartTime(r.getProposedTime());
                if (classEntity.getDuration() != null) {
                    classEntity.setEndTime(r.getProposedTime().plusSeconds(classEntity.getDuration() * 60L));
                }
                classRepository.save(classEntity);
            }

            List<Enrollment> enrollments = enrollmentRepository.findByClassId(r.getClassId());
            for (Enrollment e : enrollments) {
                RescheduleResponse existing = rescheduleResponseRepository
                        .findByRescheduleIdAndUserId(rescheduleId, e.getBeneficiaryId()).orElse(null);
                if (existing == null) {
                    rescheduleResponseRepository.save(RescheduleResponse.builder()
                            .rescheduleId(rescheduleId)
                            .userId(e.getBeneficiaryId())
                            .responseType(null)
                            .respondedAt(null)
                            .build());
                }
                notificationService.enviar(e.getBeneficiaryId(), "reschedule", "Reagendamiento propuesto",
                        "El profesor acepto el reagendamiento para el " + r.getProposedTime() + ". Tienes 48h para confirmar o rechazar.");
            }

            log.info("Teacher {} accepted reschedule {}. Deadline: {}", teacherId, rescheduleId, r.getResponseDeadline());
        } else {
            r.setStatus(RescheduleStatus.TEACHER_REJECTED);
            r.setTeacherId(teacherId);
            rescheduleRepository.save(r);

            List<Enrollment> enrollments = enrollmentRepository.findByClassId(r.getClassId());
            int refunds = 0;
            for (Enrollment e : enrollments) {
                List<Payment> payments = paymentRepository.findByEnrollmentId(e.getId());
                for (Payment p : payments) {
                    if (p.getStatus() == PaymentStatus.RETAINED) {
                        p.setStatus(PaymentStatus.REFUND_PENDING);
                        paymentRepository.save(p);
                        refunds++;
                    }
                }
            }

            log.info("Teacher {} rejected reschedule {}. {} payments marked for refund. Class: {}",
                    teacherId, rescheduleId, refunds, r.getClassId());
        }

        return toDto(r);
    }

    @Transactional(readOnly = true)
    public RescheduleResponseDto getReschedule(UUID id) {
        return rescheduleRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDto> getStudentResponses(UUID rescheduleId) {
        return rescheduleResponseRepository.findByRescheduleId(rescheduleId).stream()
                .map(rr -> new StudentResponseDto(rr.getId(), rr.getUserId(), null, null, rr.getResponseType(), rr.getRespondedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getAvailableSlotsForReschedule(UUID classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        return roomAvailabilityService.getAvailableSlotsForReschedule(
                classEntity.getRoom().getVenue().getId().toString(),
                Instant.now());
    }

    public List<RescheduleResponseDto> getByClass(UUID classId) {
        return rescheduleRepository.findByClassId(classId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<RescheduleResponseDto> getByTeacher(UUID teacherId) {
        return rescheduleRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    @Transactional
    public void studentDecision(UUID rescheduleId, boolean accepted, UUID studentId) {
        Reschedule r = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule not found"));

        if (r.getStatus() != RescheduleStatus.TEACHER_ACCEPTED) {
            throw new BusinessException("Este reagendamiento no esta en estado para recibir respuestas de alumnos.");
        }

        RescheduleResponse rr = rescheduleResponseRepository
                .findByRescheduleIdAndUserId(rescheduleId, studentId).orElse(null);
        if (rr == null) {
            throw new BusinessException("No estas registrado como participante de este reagendamiento.");
        }

        if (rr.getResponseType() != null) {
            throw new BusinessException("Ya enviaste tu respuesta a este reagendamiento.");
        }

        rr.setResponseType(accepted ? ResponseType.ACCEPTED : ResponseType.REJECTED);
        rr.setRespondedAt(Instant.now());
        rescheduleResponseRepository.save(rr);

        if (!accepted) {
            List<Enrollment> enrollments = enrollmentRepository.findByClassId(r.getClassId());
            for (Enrollment e : enrollments) {
                if (studentId.equals(e.getBeneficiaryId())) {
                    List<Payment> payments = paymentRepository.findByEnrollmentId(e.getId());
                    for (Payment p : payments) {
                        if (p.getStatus() == PaymentStatus.RETAINED) {
                            p.setStatus(PaymentStatus.REFUND_PENDING);
                            paymentRepository.save(p);
                            log.info("Student {} rejected reschedule {}, payment {} marked for refund", studentId, rescheduleId, p.getId());
                        }
                    }
                }
            }
        }

        notificationService.enviar(studentId, "reschedule",
                accepted ? "Reagendamiento aceptado" : "Reagendamiento rechazado",
                accepted ? "Has aceptado el reagendamiento. Tu inscripcion sigue activa." : "Has rechazado el reagendamiento. Se procesara tu devolucion.");

        long pending = rescheduleResponseRepository.findByRescheduleIdAndResponseTypeIsNull(rescheduleId).size();
        if (pending == 0) {
            r.setStatus(RescheduleStatus.COMPLETED);
            rescheduleRepository.save(r);
            log.info("All students responded to reschedule {}. Marked as COMPLETED.", rescheduleId);
        }
    }

    @Transactional
    public void processTimeouts() {
        Instant now = Instant.now();
        List<Reschedule> expired = rescheduleRepository.findByStatusAndResponseDeadlineBefore(
                RescheduleStatus.TEACHER_ACCEPTED, now);

        if (expired.isEmpty()) return;

        for (Reschedule r : expired) {
            try {
                List<RescheduleResponse> pendingResponses = rescheduleResponseRepository
                        .findByRescheduleIdAndResponseTypeIsNull(r.getId());

                List<Enrollment> enrollments = enrollmentRepository.findByClassId(r.getClassId());
                int refunds = 0;
                for (RescheduleResponse rr : pendingResponses) {
                    rr.setResponseType(ResponseType.TIMEOUT);
                    rr.setRespondedAt(now);
                    rescheduleResponseRepository.save(rr);

                    for (Enrollment e : enrollments) {
                        if (rr.getUserId().equals(e.getBeneficiaryId())) {
                            List<Payment> payments = paymentRepository.findByEnrollmentId(e.getId());
                            for (Payment p : payments) {
                                if (p.getStatus() == PaymentStatus.RETAINED) {
                                    p.setStatus(PaymentStatus.REFUND_PENDING);
                                    paymentRepository.save(p);
                                    refunds++;
                                }
                            }
                        }
                    }
                }

                r.setStatus(RescheduleStatus.COMPLETED);
                rescheduleRepository.save(r);

                log.info("Reschedule {} expired. {} students timed out, {} payments marked for refund. Class: {}",
                        r.getId(), pendingResponses.size(), refunds, r.getClassId());
            } catch (Exception e) {
                log.error("Error processing reschedule timeout {}", r.getId(), e);
            }
        }
    }

    private RescheduleResponseDto toDto(Reschedule r) {
        return new RescheduleResponseDto(r.getId(), r.getClassId(), r.getTeacherId(),
                r.getProposedTime(), r.getReason(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCreatedAt());
    }
}
