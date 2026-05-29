package com.modoensayo.users.domain;

import com.modoensayo.shared.config.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "professional_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfessionalProfile {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String description;
    private String photoUrl;
    private Double averageRating;
    private String specialty;
    private Integer experienceYears;
    private String especialidad;
    private String nivelEnsenanza;
    private String formacion;
    private String instagram;
    private String youtube;
    private String sitioWeb;
    private String linkedin;

    /** Texto libre de presentación del profesor. */
    @Column(columnDefinition = "TEXT")
    private String biografia;

    /** Disciplina principal declarada por el profesor. */
    private String disciplinaPrincipal;

    /** Disciplinas secundarias (almacenadas separadas por "||"). */
    @Convert(converter = StringListConverter.class)
    @Column(name = "disciplinas_secundarias", length = 1000)
    @Builder.Default
    private List<String> disciplinasSecundarias = Collections.emptyList();

    /** Tipos de formación (almacenados separados por "||"). */
    @Convert(converter = StringListConverter.class)
    @Column(name = "tipo_formacion", length = 500)
    @Builder.Default
    private List<String> tipoFormacion = Collections.emptyList();

    /** Detalle adicional sobre la formación del profesor. */
    @Column(columnDefinition = "TEXT")
    private String detalleFormacion;
}
