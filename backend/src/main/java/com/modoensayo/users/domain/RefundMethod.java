package com.modoensayo.users.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "refund_methods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundMethod extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private String bank;

    private String accountType;

    private String accountNumber;

    private String accountHolder;

    private String rut;
}
