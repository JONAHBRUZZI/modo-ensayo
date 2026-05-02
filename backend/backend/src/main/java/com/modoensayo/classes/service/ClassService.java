package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.domain.ClassStatusHistory;
import com.modoensayo.classes.dto.ClassRequest;
import com.modoensayo.classes.dto.ClassResponse;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.classes.repository.ClassStatusHistoryRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassStatusHistoryRepository statusHistoryRepository;
    private final RoomRepository roomRepository;

    public ClassService(ClassRepository classRepository,
                        ClassStatusHistoryRepository statusHistoryRepository,
                        RoomRepository roomRepository) {
        this.classRepository = classRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> listPublished() {
        return classRepository.findByStatusAndStartTimeAfter(
                        ClassStatus.PUBLISHED, Instant.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassResponse create(String teacherId, ClassRequest request) {
        Room room = roomRepository.findById(UUID.fromString(request.roomId()))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        Class classEntity = Class.builder()
                .room(room)
                .teacherId(UUID.fromString(teacherId))
                .title(request.title())
                .discipline(request.discipline())
                .capacity(request.capacity())
                .price(request.price())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ClassStatus.PUBLISHED)
                .build();

        classRepository.save(classEntity);
        return toResponse(classEntity);
    }

    @Transactional
    public ClassResponse updateStatus(String classId, String newStatus, String changedBy) {
        Class classEntity = classRepository.findById(UUID.fromString(classId))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        String previousStatus = classEntity.getStatus().name();

        try {
            classEntity.setStatus(ClassStatus.valueOf(newStatus));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid class status: " + newStatus);
        }

        classRepository.save(classEntity);

        ClassStatusHistory history = ClassStatusHistory.builder()
                .classEntity(classEntity)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(UUID.fromString(changedBy))
                .build();

        statusHistoryRepository.save(history);

        return toResponse(classEntity);
    }

    private ClassResponse toResponse(Class classEntity) {
        return new ClassResponse(
                classEntity.getId().toString(),
                classEntity.getRoom().getId().toString(),
                classEntity.getRoom().getVenue().getName(),
                classEntity.getTeacherId().toString(),
                classEntity.getTitle(),
                classEntity.getDiscipline(),
                classEntity.getCapacity(),
                classEntity.getPrice(),
                classEntity.getStartTime(),
                classEntity.getEndTime(),
                classEntity.getStatus().name()
        );
    }
}
