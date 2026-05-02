package com.modoensayo.reschedules.repository;

import com.modoensayo.reschedules.domain.RescheduleResponse;
import com.modoensayo.reschedules.enums.ResponseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescheduleResponseRepository extends JpaRepository<RescheduleResponse, UUID> {
    List<RescheduleResponse> findByRescheduleId(UUID rescheduleId);
    Optional<RescheduleResponse> findByRescheduleIdAndUserId(UUID rescheduleId, UUID userId);
    List<RescheduleResponse> findByRescheduleIdAndResponseTypeIsNull(UUID rescheduleId);
    long countByRescheduleIdAndResponseType(UUID rescheduleId, ResponseType responseType);
}
