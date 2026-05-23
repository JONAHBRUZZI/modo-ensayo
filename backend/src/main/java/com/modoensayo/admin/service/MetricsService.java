package com.modoensayo.admin.service;

import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.attendance.repository.AttendanceRepository;
import com.modoensayo.venues.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ClassRepository classRepository;
    private final AttendanceRepository attendanceRepository;
    private final VenueRepository venueRepository;

    public Map<String, Object> getTeacherMetrics(java.util.UUID teacherId) {
        Map<String, Object> m = new HashMap<>();
        var classes = classRepository.findByTeacherId(teacherId);
        m.put("totalClases", classes.size());
        m.put("asistenciaPromedio", 0);
        m.put("rating", 0);
        return m;
    }

    public List<Map<String, Object>> getTeacherEarnings(java.util.UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("classTitle", c.getTitle());
            m.put("amount", c.getPrice());
            m.put("date", c.getCreatedAt());
            m.put("status", "COMPLETED");
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    public Map<String, Object> getVenueMetrics(java.util.UUID venueId) {
        Map<String, Object> m = new HashMap<>();
        var classes = classRepository.findByRoomVenueId(venueId);
        m.put("totalClases", classes.size());
        m.put("ocupacion", 0);
        m.put("alumnos", 0);
        m.put("ingresos", classes.stream().mapToDouble(c -> c.getPrice() != null ? c.getPrice() : 0).sum());
        return m;
    }

    public List<Map<String, Object>> getVenueProfessors(java.util.UUID venueId) {
        return classRepository.findByRoomVenueId(venueId).stream()
                .filter(c -> c.getTeacherId() != null)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getTeacherId());
                    m.put("name", "Profesor");
                    m.put("email", "");
                    m.put("status", "ACTIVE");
                    return m;
                })
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
}
