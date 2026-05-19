package com.modoensayo.admin.service;

import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AdminSedeService {

    private final VenueRepository venueRepository;
    private final NotificationRepository notificationRepository;

    public AdminSedeService(VenueRepository venueRepository,
                            NotificationRepository notificationRepository) {
        this.venueRepository = venueRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Venue> getSedesPendientes() {
        return venueRepository.findByStatus(EstadoSede.PENDIENTE_APROBACION.name());
    }

    @Transactional
    public Venue aprobarSede(UUID sedeId, UUID adminId) {
        Venue venue = venueRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        venue.setStatus(EstadoSede.APROBADA.name());
        Venue saved = venueRepository.save(venue);

        if (saved.getAdminId() != null) {
            notificationRepository.save(Notification.builder()
                    .userId(saved.getAdminId())
                    .message("Tu sede '" + saved.getName() + "' ha sido aprobada.")
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

        venue.setStatus(EstadoSede.RECHAZADA.name());
        Venue saved = venueRepository.save(venue);

        if (saved.getAdminId() != null) {
            String mensaje = motivo != null && !motivo.isBlank()
                    ? "Tu sede '" + saved.getName() + "' fue rechazada. Motivo: " + motivo
                    : "Tu sede '" + saved.getName() + "' fue rechazada.";
            notificationRepository.save(Notification.builder()
                    .userId(saved.getAdminId())
                    .message(mensaje)
                    .read(false)
                    .createdAt(Instant.now())
                    .build());
        }

        return saved;
    }
}
