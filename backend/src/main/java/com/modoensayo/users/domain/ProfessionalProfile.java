package com.modoensayo.users.domain;

import jakarta.persistence.*;
import lombok.*;
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
}
