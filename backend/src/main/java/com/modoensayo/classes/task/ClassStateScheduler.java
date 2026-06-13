package com.modoensayo.classes.task;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ClassStateScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClassStateScheduler.class);

    private final ClassRepository classRepository;

    public ClassStateScheduler(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void transicionarEstados() {
        Instant now = Instant.now();

        List<Class> publishedStarted = classRepository.findByStatus(ClassStatus.PUBLISHED).stream()
                .filter(c -> c.getStartTime() != null && !c.getStartTime().isAfter(now))
                .toList();
        for (Class c : publishedStarted) {
            c.setStatus(ClassStatus.IN_PROGRESS);
            classRepository.save(c);
        }
        if (!publishedStarted.isEmpty()) {
            log.info("Transitioned {} classes from PUBLISHED to IN_PROGRESS", publishedStarted.size());
        }

        List<Class> inProgressEnded = classRepository.findByStatus(ClassStatus.IN_PROGRESS).stream()
                .filter(c -> c.getEndTime() != null && !c.getEndTime().isAfter(now))
                .toList();
        for (Class c : inProgressEnded) {
            c.setStatus(ClassStatus.POR_VALIDAR);
            classRepository.save(c);
        }
        if (!inProgressEnded.isEmpty()) {
            log.info("Transitioned {} classes from IN_PROGRESS to POR_VALIDAR", inProgressEnded.size());
        }
    }
}
