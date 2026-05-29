package com.modoensayo.users.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    private String socialName;

    private String phone;

    @Column(unique = true)
    private String rut;

    @Builder.Default
    private boolean tieneSedeAprobada = false;

    /** Persistido: true cuando el admin aprueba el documento de identidad. */
    @Builder.Default
    private boolean identidadValidada = false;

    /** Persistido: SIN_VALIDAR | PENDIENTE | APROBADO | RECHAZADO */
    @Builder.Default
    private String identidadEstado = "SIN_VALIDAR";

    @Builder.Default
    private boolean enabled = true;

    private UUID preferredRefundMethodId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();
}
