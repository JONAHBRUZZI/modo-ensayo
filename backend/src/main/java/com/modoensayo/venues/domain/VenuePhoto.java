package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Foto de una sede o de una sala.
 * El campo ownerType distingue "VENUE" de "ROOM".
 */
@Entity
@Table(name = "venue_photos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenuePhoto extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID de la sede o sala a la que pertenece. */
    @Column(nullable = false)
    private UUID ownerId;

    /** "VENUE" o "ROOM". */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String ownerType = "VENUE";

    /** URL de la foto almacenada. */
    @Column(nullable = false, length = 1000)
    private String photoUrl;

    /** Texto alternativo / descripción breve. */
    private String altText;

    /** Orden de visualización (menor = primero). */
    @Builder.Default
    private int displayOrder = 0;

    /** Si es la foto principal/portada. */
    @Builder.Default
    private boolean principal = false;
}
