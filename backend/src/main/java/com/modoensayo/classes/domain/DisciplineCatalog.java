package com.modoensayo.classes.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "discipline_catalog", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "category"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisciplineCatalog {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int sortOrder = 0;
}
