package com.modoensayo.attendance.service;

import com.modoensayo.attendance.domain.Attendance;
import com.modoensayo.attendance.dto.AttendanceRequest;
import com.modoensayo.attendance.dto.AttendanceResponse;
import com.modoensayo.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Transactional
    public List<AttendanceResponse> markAttendance(AttendanceRequest req, String markedBy) {
        List<Attendance> records = req.items().stream().map(item -> {
            Attendance a = attendanceRepository.findByClassId(req.classId()).stream()
                    .filter(ex -> ex.getBeneficiaryId().equals(item.beneficiaryId()))
                    .findFirst()
                    .orElse(Attendance.builder().classId(req.classId())
                            .beneficiaryId(item.beneficiaryId())
                            .beneficiaryType(item.beneficiaryType())
                            .build());
            a.setPresent(item.present());
            a.setMarkedBy(markedBy);
            return attendanceRepository.save(a);
        }).collect(Collectors.toList());

        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AttendanceResponse> getAttendance(UUID classId) {
        return attendanceRepository.findByClassId(classId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private AttendanceResponse toResponse(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getClassId(), a.getBeneficiaryId(),
                a.getBeneficiaryType(), a.getPresent(), a.getCreatedAt());
    }
}
