package com.modoensayo.users.service;

import com.modoensayo.users.repository.ManagementAttributeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AttributeCleanupService {

    private final ManagementAttributeRepository managementAttributeRepository;

    public AttributeCleanupService(ManagementAttributeRepository managementAttributeRepository) {
        this.managementAttributeRepository = managementAttributeRepository;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredAttributes() {
        managementAttributeRepository.deleteByFechaFinBefore(LocalDateTime.now());
    }
}
