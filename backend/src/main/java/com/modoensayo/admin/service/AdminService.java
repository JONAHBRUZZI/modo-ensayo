package com.modoensayo.admin.service;

import com.modoensayo.admin.dto.AdminStatsResponse;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.users.service.IdentityVerificationService;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.RoomRepository;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final IdentityVerificationService identityVerificationService;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;

    public List<IdentityVerificationResponse> listPendingVerifications() {
        return identityVerificationService.listPending();
    }

    @Transactional
    public IdentityVerificationResponse reviewIdentity(String verificationId, String action, String adminId) {
        return identityVerificationService.review(verificationId, action, adminId);
    }

    public List<Venue> listPendingVenues() {
        return venueRepository.findByStatus("PENDING");
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        Map<String, Long> paymentStats = new LinkedHashMap<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentStats.put(status.name(), paymentRepository.countByStatus(status));
        }

        return new AdminStatsResponse(
                userRepository.count(),
                classRepository.count(),
                venueRepository.count(),
                roomRepository.count(),
                identityVerificationRepository.countByStatus("PENDING"),
                venueRepository.countByStatus("PENDING"),
                paymentStats
        );
    }

    @Transactional
    public Venue reviewVenue(String venueId, String action) {
        Venue venue = venueRepository.findById(UUID.fromString(venueId))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        venue.setStatus(action);
        Venue savedVenue = venueRepository.save(venue);

        if (savedVenue.getAdminId() != null) {
            String message = "APPROVED".equals(action)
                    ? String.format("Tu sede '%s' fue aprobada por el equipo admin.", savedVenue.getName())
                    : String.format("Tu sede '%s' fue rechazada por el equipo admin.", savedVenue.getName());
            notificationRepository.save(Notification.builder()
                    .userId(savedVenue.getAdminId())
                    .message(message)
                    .read(false)
                    .createdAt(Instant.now())
                    .build());
        }

        return savedVenue;
    }
}
