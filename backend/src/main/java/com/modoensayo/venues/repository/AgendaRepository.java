package com.modoensayo.venues.repository;

import com.modoensayo.venues.domain.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AgendaRepository extends JpaRepository<Agenda, UUID> {
    List<Agenda> findByRoomIdAndFecha(UUID roomId, LocalDate fecha);
    List<Agenda> findByEmailMaestroOrderByFechaDesc(String emailMaestro);

    @Query("SELECT COUNT(a) > 0 FROM Agenda a WHERE a.roomId = :roomId AND a.fecha = :fecha " +
           "AND a.estado != 'CANCELADO' " +
           "AND ((a.horaInicio <= :horaFin AND a.horaFin > :horaInicio) " +
           "OR (a.horaInicio < :horaFin AND a.horaFin >= :horaInicio))")
    boolean existsOverlapping(@Param("roomId") UUID roomId, @Param("fecha") LocalDate fecha,
                              @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin);
}
