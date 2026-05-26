package com.modoensayo.classes.domain;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;
import com.modoensayo.classes.enums.TipoClase;
import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.domain.Room;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "classes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Class extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Enumerated(EnumType.STRING)
    private Disciplina discipline;

    @Enumerated(EnumType.STRING)
    private NivelClase level;

    @Column(length = 2000)
    private String description;

    private Integer capacity;

    private Integer duration;

    private Double price;

    private Integer minAge;

    private Integer maxAge;

    private Instant startTime;

    private Instant endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private UUID teacherId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ClassStatus status = ClassStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private TipoClase tipoClase;
}
