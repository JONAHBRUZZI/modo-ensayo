package com.modoensayo.users.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "identity_verifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdentityVerification extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private String documentUrl;

    @Builder.Default
    private String status = "PENDING";

    private UUID reviewedBy;
}
