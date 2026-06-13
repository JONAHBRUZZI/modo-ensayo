package com.modoensayo.reschedules.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.domain.RescheduleResponse;
import com.modoensayo.reschedules.dto.ProponerReagendamientoRequest;
import com.modoensayo.reschedules.enums.PropuestoPor;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import com.modoensayo.reschedules.enums.RespuestaAlumno;
import com.modoensayo.reschedules.enums.ResponseType;
import com.modoensayo.reschedules.repository.RescheduleRepository;
import com.modoensayo.reschedules.repository.RescheduleResponseRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReagendamientoCatalogService {

    private final RescheduleRepository rescheduleRepository;
    private final RescheduleResponseRepository responseRepository;
    private final ClassRepository classRepository;

    public ReagendamientoCatalogService(RescheduleRepository rescheduleRepository,
                                        RescheduleResponseRepository responseRepository,
                                        ClassRepository classRepository) {
        this.rescheduleRepository = rescheduleRepository;
        this.responseRepository = responseRepository;
        this.classRepository = classRepository;
    }

    @Transactional
    public Reschedule proponerReagendamiento(UUID claseId, UUID userId, PropuestoPor propuestoPor,
                                             ProponerReagendamientoRequest request) {
        Class classEntity = classRepository.findById(claseId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        if (classEntity.getStatus() == ClassStatus.CANCELLED
                || classEntity.getStatus() == ClassStatus.COMPLETED) {
            throw new BusinessException("No se puede reagendar una clase en estado "
                    + classEntity.getStatus());
        }

        classEntity.setStatus(ClassStatus.SUSPENDED);
        classRepository.save(classEntity);

        Reschedule reschedule = Reschedule.builder()
                .classId(claseId)
                .teacherId(PropuestoPor.MAESTRO.equals(propuestoPor) ? userId : classEntity.getTeacherId())
                .proposedTime(request.proposedTime())
                .reason(request.reason())
                .status(RescheduleStatus.PROPOSED)
                .createdAt(Instant.now())
                .build();

        return rescheduleRepository.save(reschedule);
    }

    @Transactional
    public RescheduleResponse responderReagendamiento(UUID reagendamientoId, UUID alumnoId,
                                                      RespuestaAlumno respuesta) {
        Reschedule reschedule = rescheduleRepository.findById(reagendamientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Reagendamiento no encontrado"));

        if (reschedule.getStatus() != RescheduleStatus.TEACHER_ACCEPTED) {
            throw new BusinessException("El reagendamiento no esta en estado de aceptacion");
        }

        if (reschedule.getResponseDeadline() != null
                && Instant.now().isAfter(reschedule.getResponseDeadline())) {
            throw new BusinessException("El plazo de respuesta ha expirado");
        }

        RescheduleResponse response = responseRepository
                .findByRescheduleIdAndUserId(reagendamientoId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro respuesta pendiente para este alumno"));

        if (respuesta == RespuestaAlumno.ACEPTADA) {
            response.setResponseType(ResponseType.ACCEPTED);
        } else {
            response.setResponseType(ResponseType.REJECTED);
        }
        response.setRespondedAt(Instant.now());

        return responseRepository.save(response);
    }

    @Transactional
    public void rechazarReagendar(UUID claseId, UUID userId, PropuestoPor propuestoPor) {
        Class classEntity = classRepository.findById(claseId)
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada"));

        Reschedule reschedule = rescheduleRepository.findByClassIdAndStatus(claseId, RescheduleStatus.PROPOSED)
                .orElseThrow(() -> new ResourceNotFoundException("No hay reagendamiento propuesto para esta clase"));

        reschedule.setStatus(RescheduleStatus.TEACHER_REJECTED);
        rescheduleRepository.save(reschedule);

        classEntity.setStatus(ClassStatus.CANCELLED);
        classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<RescheduleResponse> getPendientes(UUID alumnoId) {
        return responseRepository.findAll().stream()
                .filter(r -> alumnoId.equals(r.getUserId()) && r.getResponseType() == null)
                .toList();
    }
}
