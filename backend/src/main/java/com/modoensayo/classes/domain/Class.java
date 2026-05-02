package com.modoensayo.classes.domain;

import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.shared.config.BaseEntity;
import com.modoensayo.venues.domain.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "classes")
public class Class extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private UUID teacherId;

    private String title;
    private String discipline;
    private Integer capacity;
    private Integer price;

    private Instant startTime;
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status;
}
