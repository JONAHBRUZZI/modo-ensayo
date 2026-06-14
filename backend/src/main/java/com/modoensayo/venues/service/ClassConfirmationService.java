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
import com.modoensayo.reschedules.service.NotificationService;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassConfirmationService {

    private final ClassRepository classRepository;
    private final VenueRepository venueRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final AttendanceRepository attendanceRepository;
    private final ClassStatusHistoryRepository classStatusHistoryRepository;

    @Transactional(readOnly = true)
    public List<ClassSummaryDto> getClassesPendingConfirmation(String venueAdminId) {
        List<Venue> venues = venueRepository.findByAdminId(UUID.fromString(venueAdminId));
        List<ClassSummaryDto> result = new ArrayList<>();

        for (Venue venue : venues) {
            List<Class> classes = classRepository.findByStatusAndRoomVenueId(ClassStatus.POR_VALIDAR, venue.getId());
            for (Class classEntity : classes) {
                long attendanceCount = attendanceRepository.countByClassIdAndPresentTrue(classEntity.getId());
                result.add(new ClassSummaryDto(
                        classEntity.getId().toString(),
                        classEntity.getTitle(),
                        classEntity.getDiscipline().name(),
                        classEntity.getRoom().getId().toString(),
                        classEntity.getRoom().getName(),
                        venue.getId().toString(),
                        venue.getName(),
                        classEntity.getTeacherId().toString(),
                        classEntity.getStartTime(),
                        classEntity.getEndTime(),
                        classEntity.getCapacity(),
                        attendanceCount,
                        classEntity.getPrice() != null ? classEntity.getPrice().intValue() : null,
                        classEntity.getStatus().name()
                ));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ClassSummaryDto> getAllVenueClasses(String venueAdminId) {
        List<Venue> venues = venueRepository.findByAdminId(UUID.fromString(venueAdminId));
        List<ClassSummaryDto> result = new ArrayList<>();
        for (Venue venue : venues) {
            List<Class> classes = classRepository.findByRoomVenueId(venue.getId());
            for (Class classEntity : classes) {
                long attendanceCount = attendanceRepository.countByClassIdAndPresentTrue(classEntity.getId());
                result.add(new ClassSummaryDto(
                        classEntity.getId().toString(),
                        classEntity.getTitle(),
                        classEntity.getDiscipline().name(),
                        classEntity.getRoom().getId().toString(),
                        classEntity.getRoom().getName(),
                        venue.getId().toString(),
                        venue.getName(),
                        classEntity.getTeacherId().toString(),
                        classEntity.getStartTime(),
                        classEntity.getEndTime(),
                        classEntity.getCapacity(),
                        attendanceCount,
                        classEntity.getPrice() != null ? classEntity.getPrice().intValue() : null,
                        classEntity.getStatus().name()
                ));
            }
        }
        return result;
    }

    @Transactional
    public ClassConfirmationResult confirmClassRealized(String venueAdminId, String classId) {
        Class classEntity = classRepository.findById(UUID.fromString(classId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        validateVenueAdminAccess(venueAdminId, classEntity);

        if (classEntity.getStatus() != ClassStatus.POR_VALIDAR) {
            throw new BusinessException("La clase no esta en estado POR_VALIDAR");
        }

        classEntity.setStatus(ClassStatus.COMPLETED);
        classRepository.save(classEntity);

        classStatusHistoryRepository.save(ClassStatusHistory.builder()
                .classEntity(classEntity).previousStatus("POR_VALIDAR")
                .newStatus("COMPLETED").changedBy(UUID.fromString(venueAdminId)).build());

        List<Payment> releasedPayments = releasePaymentsForClass(classEntity.getId());

        notifyTeacher(classEntity.getTeacherId(),
                String.format("La clase '%s' fue confirmada como realizada. Pagos liberados.", classEntity.getTitle()));

        log.info("Class {} confirmed as COMPLETED by admin {}. {} payments released.",
                classId, venueAdminId, releasedPayments.size());

        return new ClassConfirmationResult(
                classEntity.getId().toString(),
                "COMPLETED",
                releasedPayments.size()
        );
    }

    @Transactional
    public ClassConfirmationResult confirmClassNotRealized(String venueAdminId, String classId) {
        Class classEntity = classRepository.findById(UUID.fromString(classId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        validateVenueAdminAccess(venueAdminId, classEntity);

        if (classEntity.getStatus() != ClassStatus.POR_VALIDAR) {
            throw new BusinessException("La clase no esta en estado POR_VALIDAR");
        }

        classEntity.setStatus(ClassStatus.SUSPENDED);
        classRepository.save(classEntity);

        classStatusHistoryRepository.save(ClassStatusHistory.builder()
                .classEntity(classEntity).previousStatus("POR_VALIDAR")
                .newStatus("SUSPENDED").changedBy(UUID.fromString(venueAdminId)).build());

        List<Payment> refundPendingPayments = markPaymentsForRefund(classEntity.getId(),
                "Clase no realizada: confirmada por administrador de sede");

        TipoClase tipo = classEntity.getTipoClase() != null ? classEntity.getTipoClase() : TipoClase.PROPIA;

        if (tipo == TipoClase.PROPIA) {
            notifyTeacher(classEntity.getTeacherId(),
                    String.format("Tu Clase Propia '%s' fue marcada como NO REALIZADA. Debes decidir si reagendar o cancelar. Ingresa a la plataforma para gestionar el reagendamiento.", classEntity.getTitle()));
        } else {
            notifyUser(UUID.fromString(venueAdminId),
                    String.format("La Clase Asignada '%s' fue marcada como NO REALIZADA. Como Administrador de Sede, debes gestionar el reagendamiento.", classEntity.getTitle()));
            notifyTeacher(classEntity.getTeacherId(),
                    String.format("La clase '%s' fue marcada como NO REALIZADA por la sede. El Administrador de Sede gestionara el reagendamiento. Recibiras una notificacion con el resultado.", classEntity.getTitle()));
        }

        log.info("Class {} ({}) marked as SUSPENDED (not realized) by admin {}. {} payments marked for refund.",
                classId, tipo, venueAdminId, refundPendingPayments.size());

        return new ClassConfirmationResult(
                classEntity.getId().toString(),
                "SUSPENDED",
                refundPendingPayments.size()
        );
    }

    private void validateVenueAdminAccess(String venueAdminId, Class classEntity) {
        Venue venue = classEntity.getRoom().getVenue();
        if (!venue.getAdminId().equals(UUID.fromString(venueAdminId))) {
            throw new BusinessException("No tienes permiso para confirmar esta clase");
        }
    }

    private List<Payment> releasePaymentsForClass(UUID classId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        List<Payment> released = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.RETAINED) {
                    payment.setStatus(PaymentStatus.RELEASED);
                    paymentRepository.save(payment);
                    released.add(payment);
                }
            }
        }
        return released;
    }

    private List<Payment> markPaymentsForRefund(UUID classId, String reason) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        List<Payment> refundPending = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.RETAINED) {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    paymentRepository.save(payment);
                    refundPending.add(payment);
                }
            }
            if (enrollment.getBeneficiaryId() != null) {
                notifyUser(enrollment.getBeneficiaryId(), reason);
            }
        }
        return refundPending;
    }

    private void notifyTeacher(UUID teacherId, String message) {
        notificationService.enviar(teacherId, null, null, message);
    }

    private void notifyUser(UUID userId, String message) {
        notificationService.enviar(userId, null, null, message);
    }

    public record ClassSummaryDto(
            String id,
            String title,
            String discipline,
            String roomId,
            String roomName,
            String venueId,
            String venueName,
            String teacherId,
            Instant startTime,
            Instant endTime,
            Integer capacity,
            Long attendanceCount,
            Integer price,
            String status
    ) {}

    public record ClassConfirmationResult(
            String classId,
            String newStatus,
            int affectedPayments
    ) {}
}
