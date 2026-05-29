package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(nullable = false)
    private String name;

    private Integer capacity;

    private Integer tamanoM2;

    private String floorType;        // mantiene compatibilidad con datos existentes

    private String type;

    private Double pricePerHour;

    @Builder.Default
    private boolean activa = true;

    // Equipamiento — espacio
    private Boolean hasMirrors;
    private Boolean tieneBarraBallet;
    private Boolean tieneAireAcondicionado;
    private Boolean tieneCalefaccion;
    private Boolean tieneInsonorizacion;

    // Equipamiento — audio/video
    private Boolean hasSound;            // mantiene compatibilidad
    private Boolean tieneAmplificacion;
    private Boolean tieneEntradaAuxiliar;
    private Boolean tieneMicrofono;
    private Boolean tieneEquipoGrabacion;

    // Instrumentos disponibles
    private Boolean tienePiano;
    private Boolean tieneGuitarra;
    private Boolean tieneBateria;

    // Legacy
    private String equipment;
    private String imageUrl;
}
