package com.modoensayo.reviews.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID classId;
    private UUID reviewerId;
    private String targetType;
    private UUID targetId;
    private Integer score;
    private String comment;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
