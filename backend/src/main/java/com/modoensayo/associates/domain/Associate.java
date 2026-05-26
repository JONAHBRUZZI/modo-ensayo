package com.modoensayo.associates.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "associates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Associate extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    private String email;

    private String name;

    private String relationship;

    private LocalDate birthDate;

    private String rut;

    @Builder.Default
    private String status = "ACTIVE";
}
