package com.modoensayo.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * TODO: RoomAvailability entity and repository have been removed.
 * This seeder is disabled. Reimplement using VenueScheduleService when ready.
 */
@Slf4j
@Component
@Order(20)
public class RoomAvailabilitySeeder implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("RoomAvailabilitySeeder: disabled. RoomAvailability entity removed. TODO: use VenueScheduleService.");
    }
}
