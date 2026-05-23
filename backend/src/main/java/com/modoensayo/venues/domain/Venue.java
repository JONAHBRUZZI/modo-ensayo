package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.enums.EstadoSede;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venue extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID adminId;

    @Column(nullable = false)
    private String name;

    private String city;

    private String address;

    private String description;

    private String imageUrl;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSede status = EstadoSede.PENDIENTE_APROBACION;
}
