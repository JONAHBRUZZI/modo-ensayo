package com.modoensayo.reschedules.task;

import com.modoensayo.reschedules.service.RescheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescheduleTimeoutProcessor {

    private final RescheduleService rescheduleService;

    /**
     * Runs every hour to process reschedule timeouts.
     * Students who haven't responded within 48h of teacher acceptance
     * automatically get TIMEOUT → refundo.
     */
    @Scheduled(fixedRate = 3600000) // every 1 hour
    public void processTimeouts() {
        log.debug("Reschedule timeout processor running...");
        try {
            rescheduleService.processTimeouts();
        } catch (Exception e) {
            log.error("Error processing reschedule timeouts", e);
        }
    }
}
