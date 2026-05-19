package com.modoensayo.classes.service;

import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Agenda;
import com.modoensayo.venues.repository.AgendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;

    public AgendaService(AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    @Transactional
    public Agenda reservar(Agenda agenda) {
        if (agenda.getFecha() == null || agenda.getHoraInicio() == null
                || agenda.getHoraFin() == null) {
            throw new BusinessException("Fecha y horas son obligatorias para reservar");
        }

        if (agenda.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede reservar en una fecha pasada");
        }

        if (agenda.getFecha().equals(LocalDate.now())
                && agenda.getHoraInicio().isBefore(LocalTime.now())) {
            throw new BusinessException("No se puede reservar en una hora pasada");
        }

        if (agenda.getHoraInicio().isAfter(agenda.getHoraFin())
                || agenda.getHoraInicio().equals(agenda.getHoraFin())) {
            throw new BusinessException("La hora de inicio debe ser anterior a la hora de fin");
        }

        boolean overlaps = agendaRepository.existsOverlapping(
                agenda.getRoomId(), agenda.getFecha(),
                agenda.getHoraInicio(), agenda.getHoraFin());
        if (overlaps) {
            throw new BusinessException("El horario seleccionado se superpone con una reserva existente");
        }

        if (agenda.getEstado() == null || agenda.getEstado().isEmpty()) {
            agenda.setEstado("CONFIRMADO");
        }

        Agenda saved = agendaRepository.save(agenda);

        simularPromocion(saved);

        return saved;
    }

    @Transactional
    public void cancelarReserva(UUID agendaId, String emailMaestro) {
        Agenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (agenda.getEmailMaestro() == null
                || !agenda.getEmailMaestro().equalsIgnoreCase(emailMaestro)) {
            throw new BusinessException("No tienes permiso para cancelar esta reserva");
        }

        if ("CANCELADO".equalsIgnoreCase(agenda.getEstado())) {
            throw new BusinessException("La reserva ya esta cancelada");
        }

        agenda.setEstado("CANCELADO");
        agendaRepository.save(agenda);
    }

    @Transactional(readOnly = true)
    public List<Agenda> getMisReservas(String emailMaestro) {
        return agendaRepository.findByEmailMaestroOrderByFechaDesc(emailMaestro);
    }

    private void simularPromocion(Agenda agenda) {
        long count = agendaRepository.findByEmailMaestroOrderByFechaDesc(agenda.getEmailMaestro()).size();
        if (count >= 10 && count % 10 == 0) {
            System.out.println("Promocion aplicada al usuario: " + agenda.getEmailMaestro());
        }
    }
}
