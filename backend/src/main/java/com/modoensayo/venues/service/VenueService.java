package com.modoensayo.venues.service;

import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.IdentityVerification;
import com.modoensayo.users.repository.IdentityVerificationRepository;
import com.modoensayo.venues.domain.*;
import com.modoensayo.venues.dto.*;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.enums.TipoDocumentoSede;
import com.modoensayo.venues.enums.TipoPiso;
import com.modoensayo.venues.enums.TipoSede;
import com.modoensayo.venues.repository.*;
import com.modoensayo.reschedules.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final NotificationService notificationService;
    private final VenueScheduleService scheduleService;

    @Cacheable("approvedVenues")
    public List<VenueResponse> listApproved() {
        return venueRepository.findByStatusOrderByCreatedAtDesc(EstadoSede.APROBADA).stream()
                .map(this::toVenueResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse create(VenueRequest req) {
        Venue v = Venue.builder()
                .name(req.name()).city(req.city()).region(req.region()).comuna(req.comuna())
                .address(req.address())
                .description(req.description()).phone(req.phone()).email(req.email())
                .status(EstadoSede.PENDIENTE_APROBACION).build();
        return toVenueResponse(venueRepository.save(v));
    }

    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse createVenueWithIdentityValidation(UUID userId, VenueRequest req) {
        validateIdentityVerified(userId);
        Venue v = Venue.builder()
                .adminId(userId)
                .name(req.name()).city(req.city()).region(req.region()).comuna(req.comuna())
                .address(req.address())
                .description(req.description()).phone(req.phone()).email(req.email())
                .tipo(req.tipo() != null ? TipoSede.valueOf(req.tipo()) : null)
                .status(EstadoSede.PENDIENTE_APROBACION).build();
        return toVenueResponse(venueRepository.save(v));
    }

    public List<VenueResponse> getMyVenues(UUID adminId) {
        return venueRepository.findByAdminId(adminId).stream()
                .map(this::toVenueResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse createVenueAdmin(UUID adminId, VenueRequest req) {
        validateIdentityVerified(adminId);
        Venue v = Venue.builder()
                .adminId(adminId).name(req.name()).city(req.city()).region(req.region()).comuna(req.comuna())
                .address(req.address())
                .description(req.description()).phone(req.phone()).email(req.email())
                .tipo(req.tipo() != null ? TipoSede.valueOf(req.tipo()) : null)
                .status(EstadoSede.APROBADA).build();
        return toVenueResponse(venueRepository.save(v));
    }

    /**
     * Registro de sede por el propio usuario — no requiere identidad verificada previa.
     * Si ya existe una sede RECHAZADA del mismo usuario, la reutiliza actualizando los datos.
     * La sede queda en PENDIENTE_APROBACION para revisión del administrador.
     */
    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse registrarSede(UUID adminId, VenueRequest req) {
        // Reutilizar sede RECHAZADA si existe — evita acumular registros basura
        Venue v = venueRepository.findByAdminId(adminId).stream()
                .filter(s -> s.getStatus() == EstadoSede.RECHAZADA)
                .findFirst()
                .orElse(null);

        if (v != null) {
            v.setName(req.name()); v.setCity(req.city());
            v.setRegion(req.region()); v.setComuna(req.comuna());
            v.setAddress(req.address());
            v.setDescription(req.description()); v.setPhone(req.phone()); v.setEmail(req.email());
            if (req.tipo() != null) v.setTipo(TipoSede.valueOf(req.tipo()));
            v.setInstagram(req.instagram()); v.setYoutube(req.youtube());
            v.setSitioWeb(req.sitioWeb()); v.setFacebook(req.facebook());
            v.setStatus(EstadoSede.PENDIENTE_APROBACION);
        } else {
            v = Venue.builder()
                    .adminId(adminId)
                    .name(req.name()).city(req.city()).region(req.region()).comuna(req.comuna())
                    .address(req.address())
                    .description(req.description()).phone(req.phone()).email(req.email())
                    .tipo(req.tipo() != null ? TipoSede.valueOf(req.tipo()) : null)
                    .instagram(req.instagram()).youtube(req.youtube())
                    .sitioWeb(req.sitioWeb()).facebook(req.facebook())
                    .status(EstadoSede.PENDIENTE_APROBACION).build();
        }
        v = venueRepository.save(v);

        notificationService.enviar(adminId, "VENUE_REGISTERED", "Sede registrada",
                "Tu solicitud de registro para \"" + v.getName() + "\" ha sido recibida. Un administrador la revisara pronto.");

        return toVenueResponse(v);
    }

    private void validateIdentityVerified(UUID userId) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userId).orElse(null);
        if (iv == null || !"APPROVED".equals(iv.getStatus())) {
            throw new BusinessException(
                "Debes validar tu identidad antes de registrar una sede. " +
                "Sube tu documento de identidad en tu perfil y espera la aprobacion del Administrador General."
            );
        }
    }

    /**
     * Actualiza datos estructurales de la sede (solo si no esta APROBADA).
     */
    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse updateVenue(UUID userId, UUID venueId, VenueRequest req) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (v.getAdminId() == null || !v.getAdminId().equals(userId)) {
            throw new BusinessException("No tienes permiso para modificar esta sede");
        }
        if (v.getStatus() == EstadoSede.APROBADA) {
            throw new BusinessException("No se pueden modificar los datos de una sede aprobada. Contacta al Administrador General si necesitas hacer cambios.");
        }
        if (req.name() != null) v.setName(req.name());
        if (req.city() != null) v.setCity(req.city());
        if (req.region() != null) v.setRegion(req.region());
        if (req.comuna() != null) v.setComuna(req.comuna());
        if (req.address() != null) v.setAddress(req.address());
        if (req.description() != null) v.setDescription(req.description());
        if (req.phone() != null) v.setPhone(req.phone());
        if (req.email() != null) v.setEmail(req.email());
        return toVenueResponse(venueRepository.save(v));
    }

    /**
     * Actualiza redes sociales / contacto web de la sede (siempre editable, incluso si APROBADA).
     */
    @Transactional
    @CacheEvict(value = "approvedVenues", allEntries = true)
    public VenueResponse updateVenueSocial(UUID userId, UUID venueId, VenueRequest req) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (v.getAdminId() == null || !v.getAdminId().equals(userId)) {
            throw new BusinessException("No tienes permiso para modificar esta sede");
        }
        if (req.instagram() != null) v.setInstagram(req.instagram());
        if (req.youtube() != null)   v.setYoutube(req.youtube());
        if (req.sitioWeb() != null)  v.setSitioWeb(req.sitioWeb());
        if (req.facebook() != null)  v.setFacebook(req.facebook());
        // También permite actualizar descripción, teléfono y email sin re-aprobación
        if (req.description() != null) v.setDescription(req.description());
        if (req.phone() != null) v.setPhone(req.phone());
        if (req.email() != null) v.setEmail(req.email());
        return toVenueResponse(venueRepository.save(v));
    }

    public List<RoomResponse> getRooms(UUID venueId) {
        return roomRepository.findByVenueId(venueId).stream()
                .map(this::toRoomResponse).collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse createRoom(UUID userId, UUID venueId, RoomRequest req) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (v.getAdminId() == null || !v.getAdminId().equals(userId)) {
            throw new BusinessException("No tienes permiso para gestionar esta sede");
        }
        if (v.getStatus() != EstadoSede.APROBADA) {
            throw new BusinessException("La sede debe estar aprobada antes de registrar salas");
        }
        Room r = Room.builder()
                .venue(v)
                .name(req.name())
                .capacity(req.capacity())
                .tamanoM2(req.tamanoM2())
                .tipoPiso(req.tipoPiso() != null ? TipoPiso.valueOf(req.tipoPiso()) : null)
                .floorType(req.floorType())
                .type(req.type())
                .pricePerHour(req.pricePerHour())
                .activa(req.activa() != null ? req.activa() : true)
                // Equipamiento — espacio
                .hasMirrors(req.hasMirrors())
                .tieneBarraBallet(req.tieneBarraBallet())
                .tieneAireAcondicionado(req.tieneAireAcondicionado())
                .tieneCalefaccion(req.tieneCalefaccion())
                .tieneInsonorizacion(req.tieneInsonorizacion())
                // Equipamiento — audio/video
                .hasSound(req.hasSound())
                .tieneAmplificacion(req.tieneAmplificacion())
                .tieneEntradaAuxiliar(req.tieneEntradaAuxiliar())
                .tieneMicrofono(req.tieneMicrofono())
                .tieneEquipoGrabacion(req.tieneEquipoGrabacion())
                // Instrumentos
                .tienePiano(req.tienePiano())
                .tieneGuitarra(req.tieneGuitarra())
                .tieneBateria(req.tieneBateria())
                // Legacy
                .equipment(req.equipment())
                .build();
        Room saved = roomRepository.save(r);
        scheduleService.generateBlocks(v.getId());
        return toRoomResponse(saved);
    }

    /**
     * Actualiza los datos de una sala existente.
     */
    @Transactional
    public RoomResponse updateRoom(UUID userId, UUID roomId, RoomRequest req) {
        Room r = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (r.getVenue() == null || r.getVenue().getAdminId() == null
                || !r.getVenue().getAdminId().equals(userId)) {
            throw new BusinessException("No tienes permiso para modificar esta sala");
        }
        if (req.name() != null)         r.setName(req.name());
        if (req.capacity() != null)     r.setCapacity(req.capacity());
        if (req.tamanoM2() != null)     r.setTamanoM2(req.tamanoM2());
        if (req.tipoPiso() != null)      r.setTipoPiso(TipoPiso.valueOf(req.tipoPiso()));
        if (req.floorType() != null)    r.setFloorType(req.floorType());
        if (req.type() != null)         r.setType(req.type());
        if (req.pricePerHour() != null) r.setPricePerHour(req.pricePerHour());
        if (req.activa() != null)       r.setActiva(req.activa());
        if (req.hasMirrors() != null)             r.setHasMirrors(req.hasMirrors());
        if (req.tieneBarraBallet() != null)       r.setTieneBarraBallet(req.tieneBarraBallet());
        if (req.tieneAireAcondicionado() != null) r.setTieneAireAcondicionado(req.tieneAireAcondicionado());
        if (req.tieneCalefaccion() != null)       r.setTieneCalefaccion(req.tieneCalefaccion());
        if (req.tieneInsonorizacion() != null)    r.setTieneInsonorizacion(req.tieneInsonorizacion());
        if (req.hasSound() != null)               r.setHasSound(req.hasSound());
        if (req.tieneAmplificacion() != null)     r.setTieneAmplificacion(req.tieneAmplificacion());
        if (req.tieneEntradaAuxiliar() != null)   r.setTieneEntradaAuxiliar(req.tieneEntradaAuxiliar());
        if (req.tieneMicrofono() != null)         r.setTieneMicrofono(req.tieneMicrofono());
        if (req.tieneEquipoGrabacion() != null)   r.setTieneEquipoGrabacion(req.tieneEquipoGrabacion());
        if (req.tienePiano() != null)             r.setTienePiano(req.tienePiano());
        if (req.tieneGuitarra() != null)          r.setTieneGuitarra(req.tieneGuitarra());
        if (req.tieneBateria() != null)           r.setTieneBateria(req.tieneBateria());
        if (req.equipment() != null)              r.setEquipment(req.equipment());
        return toRoomResponse(roomRepository.save(r));
    }

    public VenueResponse toVenueResponsePublic(Venue v) {
        return toVenueResponse(v);
    }

    private VenueResponse toVenueResponse(Venue v) {
        return new VenueResponse(
                v.getId(), v.getName(), v.getCity(), v.getRegion(), v.getComuna(),
                v.getAddress(),
                v.getDescription(), v.getPhone(), v.getEmail(),
                v.getStatus().name(),
                v.getTipo() != null ? v.getTipo().name() : null,
                v.getCreatedAt(),
                v.getInstagram(), v.getYoutube(), v.getSitioWeb(), v.getFacebook());
    }

    /**
     * Valida que los tipos de documento enviados cubran los requeridos por el tipo de sede segun R22.
     * SEDE requiere: RUT_EMPRESA, INICIO_ACTIVIDADES_F4415, CERTIFICADO_SITUACION_TRIBUTARIA,
     *                CARPETA_TRIBUTARIA_ELECTRONICA, ESCRITURA_CONSTITUCION, CONTRATO_ARRIENDO.
     * HOME_STUDIO requiere: CEDULA_IDENTIDAD, INICIO_ACTIVIDADES_F4415,
     *                       CERTIFICADO_SITUACION_TRIBUTARIA, CONTRATO_ARRIENDO.
     * Lanza BusinessException si falta alguno requerido.
     */
    public void validarDocumentosRequeridos(String tipoSede, java.util.List<String> tiposDocumento) {
        java.util.Set<String> presentes = tiposDocumento != null
                ? new java.util.HashSet<>(tiposDocumento) : java.util.Collections.emptySet();

        java.util.List<String> requeridos;
        if ("SEDE".equals(tipoSede)) {
            requeridos = java.util.List.of(
                    TipoDocumentoSede.RUT_EMPRESA.name(),
                    TipoDocumentoSede.INICIO_ACTIVIDADES_F4415.name(),
                    TipoDocumentoSede.CERTIFICADO_SITUACION_TRIBUTARIA.name(),
                    TipoDocumentoSede.CARPETA_TRIBUTARIA_ELECTRONICA.name(),
                    TipoDocumentoSede.ESCRITURA_CONSTITUCION.name(),
                    TipoDocumentoSede.CONTRATO_ARRIENDO.name());
        } else if ("HOME_STUDIO".equals(tipoSede)) {
            requeridos = java.util.List.of(
                    TipoDocumentoSede.CEDULA_IDENTIDAD.name(),
                    TipoDocumentoSede.INICIO_ACTIVIDADES_F4415.name(),
                    TipoDocumentoSede.CERTIFICADO_SITUACION_TRIBUTARIA.name(),
                    TipoDocumentoSede.CONTRATO_ARRIENDO.name());
        } else {
            return;
        }

        java.util.List<String> faltantes = requeridos.stream()
                .filter(r -> !presentes.contains(r))
                .collect(java.util.stream.Collectors.toList());
        if (!faltantes.isEmpty()) {
            throw new BusinessException("Faltan documentos requeridos: " +
                    String.join(", ", faltantes).replace('_', ' ').toLowerCase());
        }
    }

    private RoomResponse toRoomResponse(Room r) {
        return new RoomResponse(
                r.getId(),
                r.getVenue().getId(), r.getVenue().getName(),
                r.getName(), r.getCapacity(), r.getTamanoM2(),
                r.getTipoPiso() != null ? r.getTipoPiso().name() : r.getFloorType(),
                r.getFloorType(), r.getType(), r.getPricePerHour(), r.isActiva(),
                r.getHasMirrors(), r.getTieneBarraBallet(),
                r.getTieneAireAcondicionado(), r.getTieneCalefaccion(), r.getTieneInsonorizacion(),
                r.getHasSound(), r.getTieneAmplificacion(), r.getTieneEntradaAuxiliar(),
                r.getTieneMicrofono(), r.getTieneEquipoGrabacion(),
                r.getTienePiano(), r.getTieneGuitarra(), r.getTieneBateria(),
                r.getEquipment(), r.getImageUrl(),
                r.getCreatedAt());
    }
}
