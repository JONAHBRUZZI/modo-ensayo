package com.modoensayo.shared.config;

import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.RoomAvailability;
import com.modoensayo.venues.repository.RoomAvailabilityRepository;
import com.modoensayo.venues.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Siembra bloques de disponibilidad futuros para cada sala que no tenga ninguno,
 * de modo que los maestros puedan reagendar sus clases. Es idempotente: no toca
 * las salas que ya tienen disponibilidad configurada por su Admin de Sede.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RoomAvailabilitySeeder implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final RoomAvailabilityRepository availabilityRepository;

    // Bloques tipo: tarde y noche, en los proximos dias.
    private static final LocalTime[][] BLOQUES = {
            {LocalTime.of(10, 0), LocalTime.of(12, 0)},
            {LocalTime.of(16, 0), LocalTime.of(18, 0)},
            {LocalTime.of(19, 0), LocalTime.of(21, 0)},
    };

    @Override
    public void run(String... args) {
        List<Room> rooms = roomRepository.findAll();
        ZoneId zona = ZoneId.systemDefault();
        int creados = 0;

        for (Room room : rooms) {
            if (!availabilityRepository.findByRoomId(room.getId()).isEmpty()) {
                continue; // ya tiene disponibilidad — respetamos lo configurado
            }
            // Crear bloques para los proximos 14 dias (cada 3 dias, un bloque rotando)
            for (int dia = 2; dia <= 14; dia += 3) {
                LocalDate fecha = LocalDate.now(zona).plusDays(dia);
                LocalTime[] bloque = BLOQUES[(dia / 3) % BLOQUES.length];
                Instant start = fecha.atTime(bloque[0]).atZone(zona).toInstant();
                Instant end = fecha.atTime(bloque[1]).atZone(zona).toInstant();
                availabilityRepository.save(RoomAvailability.builder()
                        .room(room).startTime(start).endTime(end).build());
                creados++;
            }
        }
        if (creados > 0) {
            log.info("RoomAvailabilitySeeder: creados {} bloques de disponibilidad para salas sin agenda.", creados);
        }
    }
}
