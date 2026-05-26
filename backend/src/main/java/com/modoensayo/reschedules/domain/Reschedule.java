package com.modoensayo.reschedules.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reschedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reschedule {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID classId;
    private UUID teacherId;
    private Instant proposedTime;
    private String reason;

    @Builder.Default
    private String status = "PROPOSED";

    private Instant responseDeadline;
    private UUID newClassId;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
