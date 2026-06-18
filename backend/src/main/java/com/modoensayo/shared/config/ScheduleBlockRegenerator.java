package com.modoensayo.shared.config;

import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.domain.VenueSchedule;
import com.modoensayo.venues.repository.VenueRepository;
import com.modoensayo.venues.service.VenueScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleBlockRegenerator {
    private final VenueRepository venueRepo;
    private final VenueScheduleService scheduleService;

    @Scheduled(cron = "0 0 4 * * 1")
    public void regenerateAllVenues() {
        List<Venue> venues = venueRepo.findAll();
        for (Venue v : venues) {
            try {
                List<VenueSchedule> schedules = scheduleService.getSchedule(v.getId());
                if (!schedules.isEmpty()) {
                    scheduleService.generateBlocks(v.getId());
                    log.info("Regenerated blocks for venue {}", v.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to regenerate blocks for venue {}", v.getId(), e);
            }
        }
    }
}
