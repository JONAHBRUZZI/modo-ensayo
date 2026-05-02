package com.modoensayo.classes.task;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassCompletionProcessor {

    private final ClassRepository classRepository;

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void completePastClasses() {
        Instant now = Instant.now();
        List<Class> publishedPast = classRepository.findByStatusAndEndTimeBefore(ClassStatus.PUBLISHED, now);
        for (Class classEntity : publishedPast) {
            classEntity.setStatus(ClassStatus.POR_VALIDAR);
            classRepository.save(classEntity);
        }
        if (!publishedPast.isEmpty()) {
            log.info("Marked {} classes as POR_VALIDAR", publishedPast.size());
        }
    }
}
