package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.enums.EstadoSede;
import com.modoensayo.venues.enums.TipoSede;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "venues", indexes = {
    @Index(name = "idx_venue_status",   columnList = "status"),
    @Index(name = "idx_venue_admin_id", columnList = "admin_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venue extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID adminId;

    @Column(nullable = false)
    private String name;

    private String city;

    private String region;

    private String comuna;

    private String address;

    private String description;

    private String imageUrl;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSede status = EstadoSede.PENDIENTE_APROBACION;

    @Enumerated(EnumType.STRING)
    private TipoSede tipo;

    @Column(length = 1000)
    private String rejectionReason;

    // Redes sociales y contacto web
    private String instagram;
    private String youtube;
    private String sitioWeb;
    private String facebook;
}
