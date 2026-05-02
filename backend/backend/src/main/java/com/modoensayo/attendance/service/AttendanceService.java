package com.modoensayo.attendance.service;

import com.modoensayo.attendance.domain.Attendance;
import com.modoensayo.attendance.dto.AttendanceRequest;
import com.modoensayo.attendance.dto.AttendanceResponse;
import com.modoensayo.attendance.repository.AttendanceRepository;
import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;

    @Transactional
    public List<AttendanceResponse> markAttendance(String teacherId, AttendanceRequest request) {
        Class classEntity = classRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!teacherId.equals(classEntity.getTeacherId().toString())) {
            throw new BusinessException("Solo el profesor de la clase puede marcar asistencia");
        }

        if (classEntity.getStatus() != ClassStatus.PUBLISHED) {
            throw new BusinessException("Solo se puede marcar asistencia en clases publicadas");
        }

        List<AttendanceResponse> results = new java.util.ArrayList<>();
        for (AttendanceRequest.AttendanceItem item : request.attendees()) {
            if (attendanceRepository.existsByClassIdAndBeneficiaryId(request.classId(), item.beneficiaryId())) {
                log.warn("Attendance already marked for beneficiary {} in class {}", item.beneficiaryId(), request.classId());
                continue;
            }

            Attendance attendance = Attendance.builder()
                    .classId(request.classId())
                    .beneficiaryId(item.beneficiaryId())
                    .beneficiaryType(item.beneficiaryType())
                    .present(item.present())
                    .markedBy(request.markedBy())
                    .build();

            attendanceRepository.save(attendance);
            results.add(toResponse(attendance));
        }

        log.info("Marked attendance for {} students in class {}", results.size(), request.classId());
        return results;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByClass(UUID classId) {
        return attendanceRepository.findByClassId(classId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getAttendanceCount(UUID classId) {
        return attendanceRepository.countByClassIdAndPresentTrue(classId);
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getClassId(),
                attendance.getBeneficiaryId(),
                attendance.getBeneficiaryType(),
                attendance.getPresent(),
                attendance.getMarkedBy()
        );
    }
}
