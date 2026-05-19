package com.modoensayo.classes.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.dto.CursoSearchDto;
import com.modoensayo.classes.dto.SearchRequest;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.venues.domain.Room;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.AgendaRepository;
import com.modoensayo.venues.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final RoomRepository roomRepository;
    private final ClassRepository classRepository;
    private final AgendaRepository agendaRepository;

    public SearchService(RoomRepository roomRepository,
                         ClassRepository classRepository,
                         AgendaRepository agendaRepository) {
        this.roomRepository = roomRepository;
        this.classRepository = classRepository;
        this.agendaRepository = agendaRepository;
    }

    public List<Object> search(SearchRequest request) {
        if (request == null) return new ArrayList<>();

        request.normalize();
        List<Object> combinedResults = new ArrayList<>();

        if (request.getTipo() == null || request.getTipo().equalsIgnoreCase("SALA")) {
            List<Room> rooms = findAllRooms();
            rooms = filterApprovedVenueRooms(rooms);

            if (request.hasComuna()) {
                rooms = filterByComuna(rooms, request.getComuna());
            }
            if (request.hasRegion()) {
                rooms = filterByRegion(rooms, request.getRegion());
            }
            if (request.getRequiereEspejos() != null && request.getRequiereEspejos()) {
                rooms = rooms.stream().filter(Room::getHasMirrors).toList();
            }
            if (request.getCapacidadMinima() != null && request.getCapacidadMinima() > 0) {
                rooms = rooms.stream()
                        .filter(r -> r.getCapacity() >= request.getCapacidadMinima())
                        .toList();
            }
            if (request.hasTimeFilter()) {
                rooms = rooms.stream()
                        .filter(room -> !agendaRepository.existsOverlapping(
                                room.getId(), request.getFecha(),
                                request.getHoraInicio(), request.getHoraFin()))
                        .toList();
            }
            if (request.hasKeyword()) {
                String kw = request.getKeyword().toLowerCase();
                rooms = rooms.stream()
                        .filter(r -> matchesKeyword(r, kw))
                        .toList();
            }

            for (Room room : rooms) {
                combinedResults.add(toSalaSearchResult(room));
            }
        }

        if (request.getTipo() == null || request.getTipo().equalsIgnoreCase("CURSO")) {
            List<Class> courses = findAllClasses();

            courses = courses.stream()
                    .filter(c -> c.getStatus() == ClassStatus.PUBLISHED)
                    .filter(c -> c.getRoom() != null && c.getRoom().getVenue() != null
                            && "APPROVED".equalsIgnoreCase(c.getRoom().getVenue().getStatus()))
                    .toList();

            if (request.hasComuna()) {
                courses = filterClassesByComuna(courses, request.getComuna());
            }
            if (request.hasRegion()) {
                courses = filterClassesByRegion(courses, request.getRegion());
            }
            if (request.getDisciplina() != null) {
                String discName = request.getDisciplina().name();
                courses = courses.stream()
                        .filter(c -> c.getDiscipline() != null
                                && c.getDiscipline().equalsIgnoreCase(discName))
                        .toList();
            }
            if (request.getNivel() != null && !request.getNivel().isBlank()) {
                try {
                    NivelClase nivelEnum = NivelClase.valueOf(request.getNivel().toUpperCase());
                    courses = courses.stream()
                            .filter(c -> c.getDiscipline() != null)
                            .toList();
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (request.hasKeyword()) {
                String kw = request.getKeyword().toLowerCase();
                courses = courses.stream()
                        .filter(c -> c.getTitle() != null
                                && c.getTitle().toLowerCase().contains(kw))
                        .toList();
            }

            for (Class c : courses) {
                combinedResults.add(toCursoSearchDto(c));
            }
        }

        return combinedResults;
    }

    private List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    private List<Room> filterApprovedVenueRooms(List<Room> rooms) {
        return rooms.stream()
                .filter(r -> r.getVenue() != null
                        && "APPROVED".equalsIgnoreCase(r.getVenue().getStatus()))
                .toList();
    }

    private List<Room> filterByComuna(List<Room> rooms, String comuna) {
        String c = comuna.toLowerCase();
        return rooms.stream()
                .filter(r -> r.getVenue() != null
                        && r.getVenue().getAddress() != null
                        && r.getVenue().getAddress().toLowerCase().contains(c))
                .toList();
    }

    private List<Room> filterByRegion(List<Room> rooms, String region) {
        String r = region.toLowerCase();
        return rooms.stream()
                .filter(room -> room.getVenue() != null
                        && room.getVenue().getAddress() != null
                        && room.getVenue().getAddress().toLowerCase().contains(r))
                .toList();
    }

    private List<Class> filterClassesByComuna(List<Class> courses, String comuna) {
        String c = comuna.toLowerCase();
        return courses.stream()
                .filter(cl -> cl.getRoom() != null && cl.getRoom().getVenue() != null
                        && cl.getRoom().getVenue().getAddress() != null
                        && cl.getRoom().getVenue().getAddress().toLowerCase().contains(c))
                .toList();
    }

    private List<Class> filterClassesByRegion(List<Class> courses, String region) {
        String r = region.toLowerCase();
        return courses.stream()
                .filter(cl -> cl.getRoom() != null && cl.getRoom().getVenue() != null
                        && cl.getRoom().getVenue().getAddress() != null
                        && cl.getRoom().getVenue().getAddress().toLowerCase().contains(r))
                .toList();
    }

    private boolean matchesKeyword(Room room, String keyword) {
        Venue venue = room.getVenue();
        return (room.getName() != null && room.getName().toLowerCase().contains(keyword))
                || (venue != null && venue.getName() != null
                    && venue.getName().toLowerCase().contains(keyword))
                || (venue != null && venue.getAddress() != null
                    && venue.getAddress().toLowerCase().contains(keyword));
    }

    private List<Class> findAllClasses() {
        return classRepository.findAll();
    }

    private SalaSearchResult toSalaSearchResult(Room room) {
        Venue venue = room.getVenue();
        return new SalaSearchResult(
                room.getId(),
                room.getName(),
                venue != null ? venue.getAddress() : null,
                room.getCapacity(),
                room.getHasMirrors(),
                room.getFloorType(),
                venue != null ? venue.getId() : null,
                venue != null ? venue.getName() : null,
                venue != null ? venue.getAddress() : null,
                venue != null ? venue.getAddress() : null
        );
    }

    private CursoSearchDto toCursoSearchDto(Class classEntity) {
        Room room = classEntity.getRoom();
        Venue venue = room != null ? room.getVenue() : null;

        LocalDateTime startLdt = classEntity.getStartTime() != null
                ? LocalDateTime.ofInstant(classEntity.getStartTime(), ZoneId.systemDefault())
                : null;
        LocalDateTime endLdt = classEntity.getEndTime() != null
                ? LocalDateTime.ofInstant(classEntity.getEndTime(), ZoneId.systemDefault())
                : null;

        Disciplina disciplina = null;
        if (classEntity.getDiscipline() != null) {
            try {
                disciplina = Disciplina.valueOf(classEntity.getDiscipline().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        BigDecimal precio = classEntity.getPrice() != null
                ? BigDecimal.valueOf(classEntity.getPrice()) : null;

        return new CursoSearchDto(
                classEntity.getId(),
                classEntity.getTitle(),
                disciplina,
                null,
                precio,
                classEntity.getCapacity(),
                0,
                classEntity.getCapacity(),
                startLdt,
                endLdt,
                venue != null ? venue.getName() : null,
                room != null ? room.getName() : null,
                venue != null ? venue.getAddress() : null,
                venue != null ? venue.getAddress() : null
        );
    }

    public static class SalaSearchResult {
        private UUID id;
        private String nombre;
        private String direccion;
        private Integer capacidad;
        private Boolean tieneEspejos;
        private String tipoPiso;
        private UUID sedeId;
        private String nombreSede;
        private String direccionSede;
        private String comunaSede;

        public SalaSearchResult() {}

        public SalaSearchResult(UUID id, String nombre, String direccion,
                                Integer capacidad, Boolean tieneEspejos,
                                String tipoPiso, UUID sedeId, String nombreSede,
                                String direccionSede, String comunaSede) {
            this.id = id;
            this.nombre = nombre;
            this.direccion = direccion;
            this.capacidad = capacidad;
            this.tieneEspejos = tieneEspejos;
            this.tipoPiso = tipoPiso;
            this.sedeId = sedeId;
            this.nombreSede = nombreSede;
            this.direccionSede = direccionSede;
            this.comunaSede = comunaSede;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
        public Integer getCapacidad() { return capacidad; }
        public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
        public Boolean getTieneEspejos() { return tieneEspejos; }
        public void setTieneEspejos(Boolean tieneEspejos) { this.tieneEspejos = tieneEspejos; }
        public String getTipoPiso() { return tipoPiso; }
        public void setTipoPiso(String tipoPiso) { this.tipoPiso = tipoPiso; }
        public UUID getSedeId() { return sedeId; }
        public void setSedeId(UUID sedeId) { this.sedeId = sedeId; }
        public String getNombreSede() { return nombreSede; }
        public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }
        public String getDireccionSede() { return direccionSede; }
        public void setDireccionSede(String direccionSede) { this.direccionSede = direccionSede; }
        public String getComunaSede() { return comunaSede; }
        public void setComunaSede(String comunaSede) { this.comunaSede = comunaSede; }
    }
}
