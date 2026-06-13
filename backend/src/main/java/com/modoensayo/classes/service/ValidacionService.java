package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.domain.ClassStatusHistory;
import com.modoensayo.classes.enums.ClassStatus;
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
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ValidacionService {

    private final ClassRepository classRepository;
    private final ClassStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;

    public ValidacionService(ClassRepository classRepository,
                             ClassStatusHistoryRepository statusHistoryRepository,
                             PaymentRepository paymentRepository,
                             EnrollmentRepository enrollmentRepository,
                             NotificationRepository notificationRepository) {
        this.classRepository = classRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<ClasePorConfirmar> getClasesPorConfirmar(UUID sedeId) {
        List<Class> classes = classRepository.findByStatusAndRoomVenueId(ClassStatus.POR_VALIDAR, sedeId);
        List<ClasePorConfirmar> result = new ArrayList<>();
        for (Class c : classes) {
            result.add(new ClasePorConfirmar(
                    c.getId(),
                    c.getTitle(),
                    c.getDiscipline(),
                    c.getRoom() != null ? c.getRoom().getId() : null,
                    c.getRoom() != null ? c.getRoom().getName() : null,
                    c.getTeacherId(),
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getCapacity(),
                    c.getPrice()
            ));
        }
        return result;
    }

    @Transactional
    public void marcarClaseRealizada(UUID claseId, UUID adminId, UUID sedeId) {
        Class classEntity = classRepository.findById(claseId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        validateSedeAccess(classEntity, sedeId);

        if (classEntity.getStatus() != ClassStatus.POR_VALIDAR) {
            throw new BusinessException("La clase no esta en estado POR_VALIDAR");
        }

        String prevStatus = classEntity.getStatus().name();
        classEntity.setStatus(ClassStatus.COMPLETED);
        classRepository.save(classEntity);

        saveStatusHistory(classEntity, prevStatus, "COMPLETED", adminId);

        liberarPagos(classEntity.getId());

        notifyUser(classEntity.getTeacherId(),
                "La clase '" + classEntity.getTitle() + "' fue confirmada como realizada. Pagos liberados.");
    }

    @Transactional
    public void marcarClaseNoRealizada(UUID claseId, UUID adminId, UUID sedeId) {
        Class classEntity = classRepository.findById(claseId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        validateSedeAccess(classEntity, sedeId);

        if (classEntity.getStatus() != ClassStatus.POR_VALIDAR) {
            throw new BusinessException("La clase no esta en estado POR_VALIDAR");
        }

        String prevStatus = classEntity.getStatus().name();
        classEntity.setStatus(ClassStatus.SUSPENDED);
        classRepository.save(classEntity);

        saveStatusHistory(classEntity, prevStatus, "SUSPENDED", adminId);

        markPaymentsForRefund(classEntity.getId());

        notifyUser(classEntity.getTeacherId(),
                "La clase '" + classEntity.getTitle() + "' fue marcada como NO REALIZADA.");
    }

    @Transactional(readOnly = true)
    public List<HistorialValidacionEntry> getHistorialValidaciones(UUID sedeId) {
        Instant cutoff = Instant.now().minus(14, java.time.temporal.ChronoUnit.DAYS);
        List<Class> classes = classRepository.findByStatusInAndRoomVenueId(
                List.of(ClassStatus.COMPLETED, ClassStatus.SUSPENDED), sedeId);
        List<HistorialValidacionEntry> result = new ArrayList<>();
        for (Class c : classes) {
            List<ClassStatusHistory> history = statusHistoryRepository.findByClassEntityId(c.getId());
            for (ClassStatusHistory h : history) {
                if (h.getCreatedAt() != null && h.getCreatedAt().isAfter(cutoff)) {
                    result.add(new HistorialValidacionEntry(
                            c.getId(),
                            c.getTitle(),
                            h.getPreviousStatus(),
                            h.getNewStatus(),
                            h.getCreatedAt(),
                            h.getChangedBy()
                    ));
                }
            }
        }
        return result;
    }

    private void validateSedeAccess(Class classEntity, UUID sedeId) {
        if (classEntity.getRoom() == null || classEntity.getRoom().getVenue() == null
                || !classEntity.getRoom().getVenue().getId().equals(sedeId)) {
            throw new BusinessException("La clase no pertenece a esta sede");
        }
    }

    private void liberarPagos(UUID classId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        for (Enrollment enrollment : enrollments) {
            List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.RETAINED) {
                    payment.setStatus(PaymentStatus.RELEASED);
                    paymentRepository.save(payment);
                }
            }
        }
    }

    private void markPaymentsForRefund(UUID classId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassId(classId);
        for (Enrollment enrollment : enrollments) {
            List<Payment> payments = paymentRepository.findByEnrollmentId(enrollment.getId());
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.RETAINED) {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    paymentRepository.save(payment);
                }
            }
        }
    }

    private void saveStatusHistory(Class classEntity, String previousStatus,
                                   String newStatus, UUID changedBy) {
        ClassStatusHistory history = ClassStatusHistory.builder()
                .classEntity(classEntity)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .build();
        statusHistoryRepository.save(history);
    }

    private void notifyUser(UUID userId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
    }

    public static class ClasePorConfirmar {
        private UUID id;
        private String titulo;
        private String disciplina;
        private UUID salaId;
        private String nombreSala;
        private UUID profesorId;
        private Instant fechaInicio;
        private Instant fechaFin;
        private Integer capacidad;
        private Integer precio;

        public ClasePorConfirmar() {}

        public ClasePorConfirmar(UUID id, String titulo, String disciplina,
                                 UUID salaId, String nombreSala, UUID profesorId,
                                 Instant fechaInicio, Instant fechaFin,
                                 Integer capacidad, Integer precio) {
            this.id = id;
            this.titulo = titulo;
            this.disciplina = disciplina;
            this.salaId = salaId;
            this.nombreSala = nombreSala;
            this.profesorId = profesorId;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.capacidad = capacidad;
            this.precio = precio;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getDisciplina() { return disciplina; }
        public void setDisciplina(String disciplina) { this.disciplina = disciplina; }
        public UUID getSalaId() { return salaId; }
        public void setSalaId(UUID salaId) { this.salaId = salaId; }
        public String getNombreSala() { return nombreSala; }
        public void setNombreSala(String nombreSala) { this.nombreSala = nombreSala; }
        public UUID getProfesorId() { return profesorId; }
        public void setProfesorId(UUID profesorId) { this.profesorId = profesorId; }
        public Instant getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
        public Instant getFechaFin() { return fechaFin; }
        public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
        public Integer getCapacidad() { return capacidad; }
        public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
        public Integer getPrecio() { return precio; }
        public void setPrecio(Integer precio) { this.precio = precio; }
    }

    public static class HistorialValidacionEntry {
        private UUID claseId;
        private String titulo;
        private String estadoAnterior;
        private String estadoNuevo;
        private Instant fechaCambio;
        private UUID cambiadoPor;

        public HistorialValidacionEntry() {}

        public HistorialValidacionEntry(UUID claseId, String titulo,
                                        String estadoAnterior, String estadoNuevo,
                                        Instant fechaCambio, UUID cambiadoPor) {
            this.claseId = claseId;
            this.titulo = titulo;
            this.estadoAnterior = estadoAnterior;
            this.estadoNuevo = estadoNuevo;
            this.fechaCambio = fechaCambio;
            this.cambiadoPor = cambiadoPor;
        }

        public UUID getClaseId() { return claseId; }
        public void setClaseId(UUID claseId) { this.claseId = claseId; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getEstadoAnterior() { return estadoAnterior; }
        public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }
        public String getEstadoNuevo() { return estadoNuevo; }
        public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }
        public Instant getFechaCambio() { return fechaCambio; }
        public void setFechaCambio(Instant fechaCambio) { this.fechaCambio = fechaCambio; }
        public UUID getCambiadoPor() { return cambiadoPor; }
        public void setCambiadoPor(UUID cambiadoPor) { this.cambiadoPor = cambiadoPor; }
    }
}
