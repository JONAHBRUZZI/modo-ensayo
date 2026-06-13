package com.modoensayo.venues.service;

import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SedeService {

    private final VenueRepository venueRepository;
    private final NotificationRepository notificationRepository;

    public SedeService(VenueRepository venueRepository,
                       NotificationRepository notificationRepository) {
        this.venueRepository = venueRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Venue> listAll() {
        return venueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Venue getById(UUID id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
    }

    @Transactional
    public Venue create(Venue venue) {
        validateUniqueNameAndAddress(venue.getName(), venue.getAddress(), null);
        venue.setStatus("PENDING");
        return venueRepository.save(venue);
    }

    @Transactional
    public Venue update(UUID id, Venue updated) {
        Venue existing = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        validateUniqueNameAndAddress(updated.getName(), updated.getAddress(), id);

        existing.setName(updated.getName());
        existing.setAddress(updated.getAddress());
        existing.setDescription(updated.getDescription());
        existing.setImageUrl(updated.getImageUrl());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());

        return venueRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!venueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sede no encontrada");
        }
        venueRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Venue> getSedesPendientes() {
        return venueRepository.findByStatus("PENDING");
    }

    @Transactional(readOnly = true)
    public List<Venue> getSedesByEstado(EstadoSede estado) {
        String statusStr = mapEstadoToString(estado);
        return venueRepository.findByStatus(statusStr);
    }

    @Transactional
    public Venue aprobarSede(UUID sedeId, UUID adminId) {
        Venue venue = venueRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (!"PENDING".equalsIgnoreCase(venue.getStatus())) {
            throw new BusinessException("Solo se pueden aprobar sedes en estado pendiente");
        }

        venue.setStatus("APPROVED");
        Venue saved = venueRepository.save(venue);

        if (saved.getAdminId() != null) {
            String message = "Tu sede '" + saved.getName() + "' fue aprobada.";
            notificationRepository.save(Notification.builder()
                    .userId(saved.getAdminId())
                    .message(message)
                    .read(false)
                    .createdAt(Instant.now())
                    .build());
        }

        return saved;
    }

    @Transactional
    public Venue rechazarSede(UUID sedeId, String motivo, UUID adminId) {
        Venue venue = venueRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (!"PENDING".equalsIgnoreCase(venue.getStatus())) {
            throw new BusinessException("Solo se pueden rechazar sedes en estado pendiente");
        }

        venue.setStatus("REJECTED");
        Venue saved = venueRepository.save(venue);

        if (saved.getAdminId() != null) {
            String message = "Tu sede '" + saved.getName() + "' fue rechazada. Motivo: " + motivo;
            notificationRepository.save(Notification.builder()
                    .userId(saved.getAdminId())
                    .message(message)
                    .read(false)
                    .createdAt(Instant.now())
                    .build());
        }

        return saved;
    }

    @Transactional
    public Venue reenviarSede(UUID sedeId, Venue updated, UUID arrendadorId) {
        Venue venue = venueRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (venue.getAdminId() == null || !venue.getAdminId().equals(arrendadorId)) {
            throw new BusinessException("No tienes permiso para reenviar esta sede");
        }

        if (!"REJECTED".equalsIgnoreCase(venue.getStatus())) {
            throw new BusinessException("Solo se pueden reenviar sedes rechazadas");
        }

        validateUniqueNameAndAddress(updated.getName(), updated.getAddress(), sedeId);

        venue.setName(updated.getName());
        venue.setAddress(updated.getAddress());
        venue.setDescription(updated.getDescription());
        venue.setImageUrl(updated.getImageUrl());
        venue.setPhone(updated.getPhone());
        venue.setEmail(updated.getEmail());
        venue.setStatus("PENDING");

        return venueRepository.save(venue);
    }

    private void validateUniqueNameAndAddress(String name, String address, UUID excludeId) {
        List<Venue> all = venueRepository.findAll();
        for (Venue v : all) {
            if (excludeId != null && v.getId().equals(excludeId)) {
                continue;
            }
            if (v.getName() != null && v.getName().equalsIgnoreCase(name)) {
                throw new BusinessException("Ya existe una sede con el nombre '" + name + "'");
            }
            if (v.getAddress() != null && address != null
                    && v.getAddress().equalsIgnoreCase(address)) {
                throw new BusinessException("Ya existe una sede con la direccion '" + address + "'");
            }
        }
    }

    private String mapEstadoToString(EstadoSede estado) {
        switch (estado) {
            case APROBADA:
                return "APPROVED";
            case RECHAZADA:
                return "REJECTED";
            case PENDIENTE_APROBACION:
            default:
                return "PENDING";
        }
    }
}
