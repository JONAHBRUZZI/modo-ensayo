package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "venue_block_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueBlockConfig extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_id", nullable = false, unique = true)
    private UUID venueId;

    @Column(name = "block_duration_min", nullable = false)
    @Builder.Default
    private Integer blockDurationMin = 60;

    @Column(name = "gap_between_blocks_min", nullable = false)
    @Builder.Default
    private Integer gapBetweenBlocksMin = 15;
}
