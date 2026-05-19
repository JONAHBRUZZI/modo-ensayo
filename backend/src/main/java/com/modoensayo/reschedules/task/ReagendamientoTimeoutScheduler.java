package com.modoensayo.reschedules.task;

import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.domain.RescheduleResponse;
import com.modoensayo.reschedules.enums.RescheduleStatus;
import com.modoensayo.reschedules.enums.ResponseType;
import com.modoensayo.reschedules.repository.RescheduleRepository;
import com.modoensayo.reschedules.repository.RescheduleResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ReagendamientoTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReagendamientoTimeoutScheduler.class);

    private final RescheduleRepository rescheduleRepository;
    private final RescheduleResponseRepository responseRepository;

    public ReagendamientoTimeoutScheduler(RescheduleRepository rescheduleRepository,
                                          RescheduleResponseRepository responseRepository) {
        this.rescheduleRepository = rescheduleRepository;
        this.responseRepository = responseRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void processPendingTimeouts() {
        Instant now = Instant.now();

        List<Reschedule> expired = rescheduleRepository
                .findByStatusAndResponseDeadlineBefore(RescheduleStatus.TEACHER_ACCEPTED, now);

        for (Reschedule reschedule : expired) {
            List<RescheduleResponse> pendingResponses = responseRepository
                    .findByRescheduleIdAndResponseTypeIsNull(reschedule.getId());

            for (RescheduleResponse response : pendingResponses) {
                response.setResponseType(ResponseType.RECHAZADO_AUTOMATICO);
                response.setRespondedAt(now);
                responseRepository.save(response);
                log.info("Marked response as RECHAZADO_AUTOMATICO: rescheduleId={}, userId={}, deadline={}",
                        response.getRescheduleId(), response.getUserId(), reschedule.getResponseDeadline());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Processed reschedule timeouts for {} expired reschedules", expired.size());
        }
    }
}
