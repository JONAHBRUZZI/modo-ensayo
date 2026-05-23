package com.modoensayo.payments.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    private UUID classId;

    private String classTitle;

    private String discipline;

    private String level;

    private Double price;
}
