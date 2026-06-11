package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.enums.TipoDocumentoSede;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Documento adjunto a una sede (ej: permiso municipal, certificado sanitario,
 * escritura de arriendo). El admin de sede los sube; el Admin General los revisa
 * como parte del proceso de aprobación.
 */
@Entity
@Table(name = "venue_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueDocument extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID venueId;

    /** URL del archivo almacenado (PDF, imagen, etc.). */
    @Column(nullable = false, length = 1000)
    private String fileUrl;

    /** Tipo estructurado del documento según la regla de negocio R22. */
    @Enumerated(EnumType.STRING)
    private TipoDocumentoSede tipo;

    /** Nombre descriptivo del documento: "Permiso Municipal", "Certificado Sanitario", etc. */
    private String nombre;

    /** Tipo MIME del archivo subido (ej: "application/pdf", "image/jpeg"). */
    private String tipoArchivo;

    /** Estado: PENDIENTE | APROBADO | RECHAZADO. */
    @Builder.Default
    private String estado = "PENDIENTE";

    /** Motivo de rechazo (rellenado por el Admin General). */
    @Column(length = 500)
    private String motivoRechazo;
}
