package com.modoensayo.admin.controller;

import com.modoensayo.admin.service.AdminService;
import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.dto.IdentityVerificationResponse;
import com.modoensayo.venues.domain.VenueDocument;
import com.modoensayo.venues.dto.VenueResponse;
import com.modoensayo.venues.repository.VenueDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final VenueDocumentRepository venueDocumentRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/identity-verifications")
    public ResponseEntity<List<IdentityVerificationResponse>> getVerifications() {
        return ResponseEntity.ok(adminService.getIdentityVerifications());
    }

    @PatchMapping("/identity-verifications/{id}")
    public ResponseEntity<IdentityVerificationResponse> review(@PathVariable UUID id,
                                                                @RequestParam String action,
                                                                @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(adminService.reviewIdentity(id, action, user.getUserId()));
    }

    @GetMapping("/venues/pending")
    public ResponseEntity<List<VenueResponse>> getPendingVenues() {
        return ResponseEntity.ok(adminService.getPendingVenues());
    }

    /**
     * Lista TODAS las sedes registradas en el sistema (cualquier estado).
     * Usado por el Admin General desde la pagina /admin/sedes.
     * GET /api/admin/venues
     */
    @GetMapping("/venues")
    public ResponseEntity<List<Map<String, Object>>> getAllVenues() {
        return ResponseEntity.ok(adminService.getAllVenues());
    }

    @PatchMapping("/venues/{id}/approve")
    public ResponseEntity<VenueResponse> approveVenue(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.approveVenue(id));
    }

    @PatchMapping("/venues/{id}/reject")
    public ResponseEntity<VenueResponse> rejectVenue(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.rejectVenue(id, body.get("motivo")));
    }

    /**
     * Alterna el estado de una sede entre APROBADA y SUSPENDIDA.
     * Body: { "motivo": "..." } opcional (se usa al suspender, no al reactivar)
     * PATCH /api/admin/venues/{id}/toggle
     */
    @PatchMapping("/venues/{id}/toggle")
    public ResponseEntity<VenueResponse> toggleVenue(@PathVariable UUID id,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(adminService.toggleVenue(id, motivo));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @PostMapping("/users/{id}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        adminService.assignRole(id, body.get("roleName"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID id, @PathVariable String roleName) {
        adminService.revokeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<Void> toggleUser(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        adminService.toggleUser(id, body.get("motivo"));
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina permanentemente una cuenta de usuario.
     * Protege al admin raiz y al propio actor.
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id,
                                            @AuthenticationPrincipal CustomUserDetails actor) {
        adminService.deleteUser(id, actor.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Documentos de sedes ─────────────────────────────────────────────────

    /**
     * Lista todos los documentos de una sede (para revisión por el Admin General).
     * GET /api/admin/venues/{venueId}/documentos
     */
    @GetMapping("/venues/{venueId}/documentos")
    public ResponseEntity<List<VenueDocument>> getVenueDocuments(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueDocumentRepository.findByVenueIdOrderByCreatedAtDesc(venueId));
    }

    /**
     * Aprueba o rechaza un documento de sede.
     * PATCH /api/admin/documentos/{docId}
     * Body: { "action": "approve" | "reject", "motivo": "..." }
     */
    @PatchMapping("/documentos/{docId}")
    public ResponseEntity<VenueDocument> reviewDocument(
            @PathVariable UUID docId,
            @RequestBody Map<String, String> body) {
        VenueDocument doc = venueDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        boolean aprobado = "approve".equals(body.get("action"));
        doc.setEstado(aprobado ? "APROBADO" : "RECHAZADO");
        if (!aprobado && body.get("motivo") != null) {
            doc.setMotivoRechazo(body.get("motivo"));
        }
        return ResponseEntity.ok(venueDocumentRepository.save(doc));
    }
}
