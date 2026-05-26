package com.modoensayo.reschedules.service;

import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.domain.Reschedule;
import com.modoensayo.reschedules.dto.*;
import com.modoensayo.reschedules.repository.*;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final RescheduleRepository rescheduleRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public RescheduleResponseDto propose(RescheduleRequest req) {
        Reschedule r = Reschedule.builder()
                .classId(req.classId()).proposedTime(req.proposedTime()).reason(req.reason())
                .build();
        return toDto(rescheduleRepository.save(r));
    }

    public List<RescheduleResponseDto> getByClass(UUID classId) {
        return rescheduleRepository.findByClassId(classId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<RescheduleResponseDto> getByTeacher(UUID teacherId) {
        return rescheduleRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    private RescheduleResponseDto toDto(Reschedule r) {
        return new RescheduleResponseDto(r.getId(), r.getClassId(), r.getTeacherId(),
                r.getProposedTime(), r.getReason(), r.getStatus(), r.getCreatedAt());
    }
}
