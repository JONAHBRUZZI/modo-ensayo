package com.modoensayo.admin.service;

import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.users.repository.*;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final VenueRepository venueRepository;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("usuarios", userRepository.count());
        stats.put("sedes", venueRepository.count());
        stats.put("pendientes", identityVerificationRepository.countByStatus("PENDING"));
        stats.put("sedesPendientes", venueRepository.countByStatus(EstadoSede.PENDIENTE_APROBACION));
        return stats;
    }

    public List<IdentityVerificationResponse> getIdentityVerifications() {
        return identityVerificationRepository.findByStatus("PENDING").stream()
                .map(iv -> new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(),
                        iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public IdentityVerificationResponse reviewIdentity(UUID id, String action, UUID reviewerId) {
        IdentityVerification iv = identityVerificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));
        iv.setStatus("approve".equals(action) ? "APPROVED" : "REJECTED");
        iv.setReviewedBy(reviewerId);
        iv = identityVerificationRepository.save(iv);

        if ("approve".equals(action)) {
            User user = userRepository.findById(iv.getUserId()).orElse(null);
            if (user != null) {
                Role teachRole = roleRepository.findByName("TEACHER").orElse(null);
                if (teachRole != null) {
                    boolean hasRole = user.getUserRoles().stream()
                            .anyMatch(ur -> ur.getRole().getName().equals("TEACHER"));
                    if (!hasRole) {
                        UserRoleId uriId = new UserRoleId(user.getId(), teachRole.getId());
                        UserRole ur = new UserRole(uriId, user, teachRole);
                        userRoleRepository.save(ur);
                    }
                }
            }
        }

        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt());
    }

    public List<VenueResponse> getPendingVenues() {
        return venueRepository.findByStatusOrderByCreatedAtDesc(EstadoSede.PENDIENTE_APROBACION).stream()
                .map(v -> new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                        v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                        v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public VenueResponse approveVenue(UUID id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        v.setStatus(EstadoSede.APROBADA);
        v = venueRepository.save(v);
        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt());
    }

    @Transactional
    public VenueResponse rejectVenue(UUID id, String reason) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        v.setStatus(EstadoSede.RECHAZADA);
        v.setRejectionReason(reason);
        v = venueRepository.save(v);
        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(), v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null, v.getCreatedAt());
    }

    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("fullName", u.getFullName());
            map.put("enabled", u.isEnabled());
            map.put("roles", u.getUserRoles().stream().map(ur -> ur.getRole().getName()).collect(Collectors.toSet()));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void assignRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        boolean exists = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName().equals(roleName));
        if (!exists) {
            UserRole ur = UserRole.builder().user(user).role(role).build();
            userRoleRepository.save(ur);
        }
    }

    @Transactional
    public void revokeRole(UUID userId, String roleName) {
        userRoleRepository.findByUser_Id(userId).stream()
                .filter(ur -> ur.getRole().getName().equals(roleName))
                .findFirst()
                .ifPresent(userRoleRepository::delete);
    }
}
