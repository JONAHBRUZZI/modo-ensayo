package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    private String name;

    @Column(nullable = false)
    private Integer capacity;

    private String floorType;

    @Builder.Default
    private Boolean hasMirrors = false;

    @Builder.Default
    private Boolean hasSound = false;

    @Builder.Default
    private Boolean hasBalletBar = false;

    @Builder.Default
    private Boolean hasAirConditioning = false;

    @Builder.Default
    private Boolean hasNaturalLight = false;

    private String lighting;

    private String wallColor;

    private String imageUrl;
}
