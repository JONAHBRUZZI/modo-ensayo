package com.modoensayo.venues.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.venues.domain.VenueDocument;
import com.modoensayo.venues.domain.VenuePhoto;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.enums.TipoDocumentoSede;
import com.modoensayo.venues.repository.VenuePhotoRepository;
import com.modoensayo.venues.repository.VenueDocumentRepository;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.venues.service.ClassConfirmationService;
import com.modoensayo.venues.service.ClassConfirmationService.ClassConfirmationResult;
import com.modoensayo.venues.service.ClassConfirmationService.ClassSummaryDto;
import com.modoensayo.venues.service.VenueService;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.shared.storage.UnifiedStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/venue-admin")
@RequiredArgsConstructor
public class VenueAdminController {

    private final VenueService venueService;
    private final ClassConfirmationService classConfirmationService;
    private final VenueDocumentRepository venueDocumentRepository;
    private final VenueRepository venueRepository;
    private final VenuePhotoRepository venuePhotoRepository;
    private final UnifiedStorageService storageService;

    @GetMapping("/my-venues")
    public ResponseEntity<List<VenueResponse>> getMyVenues(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(venueService.getMyVenues(user.getUserId()));
    }

    /**
     * Devuelve la sede PENDIENTE o RECHAZADA del usuario con sus documentos ya subidos.
     * Permite pre-llenar el formulario de registro al editar una solicitud.
     * GET /api/venue-admin/venues/mi-solicitud
     */
    @GetMapping("/venues/mi-solicitud")
    public ResponseEntity<Map<String, Object>> getMiSolicitud(@AuthenticationPrincipal CustomUserDetails user) {
        return venueRepository.findByAdminId(user.getUserId()).stream()
                .filter(v -> v.getStatus() == com.modoensayo.venues.enums.EstadoSede.PENDIENTE_APROBACION
                          || v.getStatus() == com.modoensayo.venues.enums.EstadoSede.RECHAZADA)
                .max(java.util.Comparator.comparing(v -> v.getCreatedAt()))
                .map(v -> {
                    Map<String, Object> res = new java.util.HashMap<>();
                    VenueResponse vr = venueService.toVenueResponsePublic(v);
                    res.put("venue", vr);
                    // tipos de documento ya guardados para esta sede
                    List<String> tiposGuardados = venueDocumentRepository
                            .findByVenueIdOrderByCreatedAtDesc(v.getId()).stream()
                            .filter(d -> d.getTipo() != null)
                            .map(d -> d.getTipo().name())
                            .distinct()
                            .collect(java.util.stream.Collectors.toList());
                    res.put("documentosGuardados", tiposGuardados);
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/venues")
    public ResponseEntity<VenueResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.createVenueAdmin(user.getUserId(), req));
    }

    /**
     * Registro de sede con documentos adjuntos en un solo paso.
     * POST /api/venue-admin/venues/registrar  (multipart/form-data)
     * Partes: nombre, ciudad, direccion, descripcion, telefono, email, tipo (SEDE|HOME_STUDIO),
     *         instagram?, youtube?, sitioWeb?, facebook?,
     *         documentos[] (archivos), tiposDocumento[] (enum TipoDocumentoSede por índice)
     */
    @PostMapping(value = "/venues/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VenueResponse> registrarConDocumentos(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String nombre,
            @RequestParam String ciudad,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String comuna,
            @RequestParam String direccion,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam String tipo,
            @RequestParam(required = false) String instagram,
            @RequestParam(required = false) String youtube,
            @RequestParam(required = false) String sitioWeb,
            @RequestParam(required = false) String facebook,
            @RequestParam(value = "documentos", required = false) List<MultipartFile> documentos,
            @RequestParam(value = "tiposDocumento", required = false) List<String> tiposDocumento) throws java.io.IOException {

        VenueRequest req = new VenueRequest(nombre, ciudad, region, comuna, direccion, descripcion,
                telefono, email, tipo, instagram, youtube, sitioWeb, facebook);

        VenueResponse venue = venueService.registrarSede(user.getUserId(), req);

        // Persistir documentos adjuntos
        if (documentos != null && !documentos.isEmpty()) {
            for (int i = 0; i < documentos.size(); i++) {
                MultipartFile archivo = documentos.get(i);
                if (archivo == null || archivo.isEmpty()) continue;
                String fileUrl = storageService.upload(archivo, "venue-docs");
                String tipoDoc = (tiposDocumento != null && i < tiposDocumento.size())
                        ? tiposDocumento.get(i) : null;
                TipoDocumentoSede tipoEnum = null;
                try {
                    if (tipoDoc != null) tipoEnum = TipoDocumentoSede.valueOf(tipoDoc);
                } catch (IllegalArgumentException e) { log.warn("Tipo de documento de sede no reconocido: {}", tipoDoc, e); }
                venueDocumentRepository.save(VenueDocument.builder()
                        .venueId(venue.id())
                        .fileUrl(fileUrl)
                        .tipo(tipoEnum)
                        .nombre(tipoDoc != null ? tipoDoc.replace('_', ' ') : archivo.getOriginalFilename())
                        .tipoArchivo(archivo.getContentType())
                        .build());
            }
        }

        return ResponseEntity.ok(venue);
    }

    @PatchMapping("/venues/{id}")
    public ResponseEntity<VenueResponse> update(@AuthenticationPrincipal CustomUserDetails user,
                                                 @PathVariable UUID id, @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.updateVenue(user.getUserId(), id, req));
    }

    /** Actualiza redes sociales y contacto web (siempre editable, incluso si la sede esta APROBADA). */
    @PatchMapping("/venues/{id}/social")
    public ResponseEntity<VenueResponse> updateSocial(@AuthenticationPrincipal CustomUserDetails user,
                                                       @PathVariable UUID id, @RequestBody VenueRequest req) {
        return ResponseEntity.ok(venueService.updateVenueSocial(user.getUserId(), id, req));
    }

    /** Actualiza los datos de una sala existente. */
    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(@AuthenticationPrincipal CustomUserDetails user,
                                                    @PathVariable UUID roomId,
                                                    @RequestBody RoomRequest req) {
        return ResponseEntity.ok(venueService.updateRoom(user.getUserId(), roomId, req));
    }

    @GetMapping("/venues/{venueId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getRooms(venueId));
    }

    @PostMapping("/venues/{venueId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(@AuthenticationPrincipal CustomUserDetails user,
                                                    @PathVariable UUID venueId, @RequestBody RoomRequest req) {
        return ResponseEntity.ok(venueService.createRoom(user.getUserId(), venueId, req));
    }

    // ── Documentos de sede ──────────────────────────────────────────────────

    /**
     * Lista los documentos subidos para una sede.
     * GET /api/venue-admin/venues/{venueId}/documentos
     */
    @GetMapping("/venues/{venueId}/documentos")
    public ResponseEntity<List<VenueDocument>> getDocuments(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID venueId) {
        // Verificar que el usuario sea el admin de la sede
        venueRepository.findById(venueId)
                .filter(v -> user.getUserId().equals(v.getAdminId()))
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        return ResponseEntity.ok(venueDocumentRepository.findByVenueIdOrderByCreatedAtDesc(venueId));
    }

    /**
     * Sube un documento para la sede.
     * POST /api/venue-admin/venues/{venueId}/documentos
     * Body: { fileUrl, nombre, tipoArchivo }
     */
    @PostMapping("/venues/{venueId}/documentos")
    public ResponseEntity<VenueDocument> addDocument(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID venueId,
            @RequestBody Map<String, String> body) {
        venueRepository.findById(venueId)
                .filter(v -> user.getUserId().equals(v.getAdminId()))
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        VenueDocument doc = VenueDocument.builder()
                .venueId(venueId)
                .fileUrl(body.get("fileUrl"))
                .nombre(body.get("nombre"))
                .tipoArchivo(body.get("tipoArchivo"))
                .build();
        return ResponseEntity.ok(venueDocumentRepository.save(doc));
    }

    /**
     * Elimina un documento de la sede.
     * DELETE /api/venue-admin/documentos/{docId}
     */
    @DeleteMapping("/documentos/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID docId) {
        VenueDocument doc = venueDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        venueRepository.findById(doc.getVenueId())
                .filter(v -> user.getUserId().equals(v.getAdminId()))
                .orElseThrow(() -> new ResourceNotFoundException("No tienes permiso para eliminar este documento"));
        venueDocumentRepository.delete(doc);
        return ResponseEntity.noContent().build();
    }

    // ── Fotos de sede y sala ────────────────────────────────────────────────

    /**
     * Lista las fotos de una sede.
     * GET /api/venue-admin/venues/{venueId}/fotos
     */
    @GetMapping("/venues/{venueId}/fotos")
    public ResponseEntity<List<VenuePhoto>> getVenuePhotos(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venuePhotoRepository
                .findByOwnerIdAndOwnerTypeOrderByDisplayOrderAsc(venueId, "VENUE"));
    }

    /**
     * Agrega una foto a una sede.
     * POST /api/venue-admin/venues/{venueId}/fotos
     * Body: { photoUrl, altText, displayOrder, principal }
     */
    @PostMapping("/venues/{venueId}/fotos")
    public ResponseEntity<VenuePhoto> addVenuePhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID venueId,
            @RequestBody Map<String, Object> body) {
        venueRepository.findById(venueId)
                .filter(v -> user.getUserId().equals(v.getAdminId()))
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        VenuePhoto photo = VenuePhoto.builder()
                .ownerId(venueId)
                .ownerType("VENUE")
                .photoUrl((String) body.get("photoUrl"))
                .altText((String) body.get("altText"))
                .displayOrder(body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : 0)
                .principal(Boolean.TRUE.equals(body.get("principal")))
                .build();
        return ResponseEntity.ok(venuePhotoRepository.save(photo));
    }

    /**
     * Lista las fotos de una sala.
     * GET /api/venue-admin/rooms/{roomId}/fotos
     */
    @GetMapping("/rooms/{roomId}/fotos")
    public ResponseEntity<List<VenuePhoto>> getRoomPhotos(@PathVariable UUID roomId) {
        return ResponseEntity.ok(venuePhotoRepository
                .findByOwnerIdAndOwnerTypeOrderByDisplayOrderAsc(roomId, "ROOM"));
    }

    /**
     * Agrega una foto a una sala.
     * POST /api/venue-admin/rooms/{roomId}/fotos
     */
    @PostMapping("/rooms/{roomId}/fotos")
    public ResponseEntity<VenuePhoto> addRoomPhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID roomId,
            @RequestBody Map<String, Object> body) {
        VenuePhoto photo = VenuePhoto.builder()
                .ownerId(roomId)
                .ownerType("ROOM")
                .photoUrl((String) body.get("photoUrl"))
                .altText((String) body.get("altText"))
                .displayOrder(body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : 0)
                .principal(Boolean.TRUE.equals(body.get("principal")))
                .build();
        return ResponseEntity.ok(venuePhotoRepository.save(photo));
    }

    /**
     * Elimina una foto.
     * DELETE /api/venue-admin/fotos/{photoId}
     */
    @DeleteMapping("/fotos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID photoId) {
        venuePhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        venuePhotoRepository.deleteById(photoId);
        return ResponseEntity.noContent().build();
    }

    // ── Clases de la sede ───────────────────────────────────────────────────

    @GetMapping("/classes")
    public ResponseEntity<List<ClassSummaryDto>> getVenueClasses(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
            classConfirmationService.getAllVenueClasses(user.getUserId().toString()));
    }

    @GetMapping("/classes/pending-confirmation")
    public ResponseEntity<List<ClassSummaryDto>> getPendingClasses(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
            classConfirmationService.getClassesPendingConfirmation(user.getUserId().toString()));
    }

    @PostMapping("/classes/{classId}/confirm-realized")
    public ResponseEntity<ClassConfirmationResult> confirmRealized(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String classId,
            @RequestBody Map<String, Object> body) {
        Object confirm = body.get("confirmacion");
        if (confirm == null || !Boolean.TRUE.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            classConfirmationService.confirmClassRealized(user.getUserId().toString(), classId));
    }

    @PostMapping("/classes/{classId}/confirm-not-realized")
    public ResponseEntity<ClassConfirmationResult> confirmNotRealized(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String classId,
            @RequestBody Map<String, Object> body) {
        Object confirm = body.get("confirmacion");
        if (confirm == null || !Boolean.TRUE.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            classConfirmationService.confirmClassNotRealized(user.getUserId().toString(), classId));
    }
}
