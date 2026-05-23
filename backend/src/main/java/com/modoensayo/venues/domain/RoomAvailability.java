package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_availabilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomAvailability extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private Instant startTime;

    private Instant endTime;
}
