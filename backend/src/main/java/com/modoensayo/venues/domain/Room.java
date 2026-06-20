package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.enums.TipoPiso;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

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

    /** Tipo de piso (enum). floorType se mantiene para compatibilidad con registros previos. */
    @Enumerated(EnumType.STRING)
    private TipoPiso tipoPiso;

    private String floorType;        // legacy — usar tipoPiso para nuevos registros

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

    @Transient
    public Map<String, List<String>> getGroupedCharacteristics() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        List<String> danza = new ArrayList<>();
        if (Boolean.TRUE.equals(hasMirrors)) danza.add("Espejos");
        if (tipoPiso != null) danza.add("Piso " + tipoPiso.name());
        if (Boolean.TRUE.equals(tieneBarraBallet)) danza.add("Barra de ballet");
        if (Boolean.TRUE.equals(tieneAireAcondicionado)) danza.add("Aire acondicionado");
        if (Boolean.TRUE.equals(tieneCalefaccion)) danza.add("Calefacción");
        if (!danza.isEmpty()) groups.put("danza", danza);

        List<String> musica = new ArrayList<>();
        if (Boolean.TRUE.equals(tieneAmplificacion)) musica.add("Amplificación");
        if (Boolean.TRUE.equals(tieneInsonorizacion)) musica.add("Insonorización");
        if (Boolean.TRUE.equals(tieneMicrofono)) musica.add("Micrófono");
        if (Boolean.TRUE.equals(tieneEntradaAuxiliar)) musica.add("Entrada auxiliar");
        if (Boolean.TRUE.equals(tieneEquipoGrabacion)) musica.add("Equipo de grabación");
        if (!musica.isEmpty()) groups.put("musica", musica);

        List<String> instrumentos = new ArrayList<>();
        if (Boolean.TRUE.equals(tienePiano)) instrumentos.add("Piano");
        if (Boolean.TRUE.equals(tieneGuitarra)) instrumentos.add("Guitarra");
        if (Boolean.TRUE.equals(tieneBateria)) instrumentos.add("Batería");
        if (!instrumentos.isEmpty()) groups.put("instrumentos", instrumentos);

        return groups;
    }
}
