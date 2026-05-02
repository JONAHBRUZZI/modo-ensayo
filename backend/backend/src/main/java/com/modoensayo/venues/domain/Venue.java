package com.modoensayo.venues.domain;

import com.modoensayo.shared.config.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "venues")
public class Venue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID adminId;

    @Column(nullable = false)
    private String name;

    private String address;
    private String description;
    private String imageUrl;
    private String phone;
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";
}
