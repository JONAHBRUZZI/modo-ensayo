package com.modoensayo.reschedules.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String title;
    private String message;
    private String type;

    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
