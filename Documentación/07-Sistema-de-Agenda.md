# Sistema de Agenda y Calendario · Modo Ensayo

> **Versión:** 1.0 — 18-jun-2026
> **Reemplaza:** `RoomAvailability` (entidad obsoleta)

---

## 1. Descripción general

El sistema de agenda permite a cada Sede definir su horario laboral, generar automáticamente bloques de disponibilidad por sala, y gestionar el ciclo de vida completo de cada bloque (disponible → ocupado → mantención). Profesores y alumnos ven vistas de calendario adaptadas a su perfil.

---

## 2. Modelo de datos

### 2.1 `venue_schedules` — Horario laboral de la sede
| Columna | Tipo | Constraints |
|---|---|---|
| id | UUID | PK |
| venue_id | UUID | FK → venues(id) ON DELETE CASCADE |
| day_of_week | VARCHAR(10) | MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY |
| open_time | TIME | NOT NULL |
| close_time | TIME | NOT NULL |
| UNIQUE | (venue_id, day_of_week) | |

### 2.2 `venue_block_configs` — Configuración de bloques
| Columna | Tipo | Constraints |
|---|---|---|
| id | UUID | PK |
| venue_id | UUID | FK → venues(id), UNIQUE |
| block_duration_min | INT | NOT NULL, DEFAULT 60 |
| gap_between_blocks_min | INT | NOT NULL, DEFAULT 15 |

### 2.3 `room_schedule_blocks` — Bloques de horario por sala (REEMPLAZA RoomAvailability)
| Columna | Tipo | Constraints |
|---|---|---|
| id | UUID | PK |
| room_id | UUID | FK → rooms(id) ON DELETE CASCADE |
| start_time | TIMESTAMPTZ | NOT NULL |
| end_time | TIMESTAMPTZ | NOT NULL |
| status | VARCHAR(15) | NOT NULL, CHECK IN ('AVAILABLE','OCCUPIED','MAINTENANCE') |
| class_id | UUID | FK → classes(id), nullable (solo si OCCUPIED) |
| INDEX | (room_id, start_time, status) | |

### 2.4 `room_maintenances` — Registro histórico de mantenciones
| Columna | Tipo | Constraints |
|---|---|---|
| id | UUID | PK |
| room_id | UUID | FK → rooms(id) |
| start_time | TIMESTAMPTZ | NOT NULL |
| end_time | TIMESTAMPTZ | NOT NULL |
| reason | TEXT | |
| created_by | UUID | FK → users(id) |
| created_at | TIMESTAMPTZ | DEFAULT now() |

---

## 3. Estados de bloque

```
AVAILABLE ──┬──→ OCCUPIED (al agendar clase)
            ├──→ MAINTENANCE (admin marca mantención)
            └── (se mantiene)

OCCUPIED ────→ AVAILABLE (al cancelar clase o reagendar)

MAINTENANCE ──→ AVAILABLE (admin libera)
```

---

## 4. Reglas de negocio

| ID | Regla |
|---|---|
| AS-01 | Al crear una sede, el sistema solicita configurar horario laboral (días + apertura/cierre) y duración de bloques |
| AS-02 | Al crear una sala, el sistema genera automáticamente `RoomScheduleBlock` para los próximos 30 días según el horario de la sede |
| AS-03 | Al editar el horario laboral, se requiere confirmación explícita con advertencia de impacto. Se regeneran todos los bloques AVAILABLE; los OCCUPIED y MAINTENANCE se mantienen |
| AS-04 | Un bloque OCCUPIED no puede ser modificado por el Admin de Sede (solo cancelando la clase asociada) |
| AS-05 | Un bloque MAINTENANCE puede ser liberado manualmente por el Admin a AVAILABLE |
| AS-06 | Al agendar una clase, el sistema valida que el bloque esté AVAILABLE y lo marca OCCUPIED atómicamente |
| AS-07 | Los bloques se regeneran automáticamente cada 7 días para mantener 30 días hacia adelante (tarea programada) |
| AS-08 | El Admin de Sede ve todos los estados. El Maestro ve solo AVAILABLE. El Alumno ve sus clases en OCCUPIED |

---

## 5. Vistas por perfil

### Admin Sede
- Calendario semanal de cada sala con todos los estados (AVAILABLE / OCCUPIED / MAINTENANCE)
- Click en bloque ocupado → ver detalle clase
- Click en bloque libre → marcar mantención (con motivo)
- Click en bloque mantención → liberar
- Editar horario laboral (con confirmación)
- Filtrar por sala individual o ver todas

### Maestro Independiente
- Buscador con filtros checkboxes (disciplina, características de sala)
- Calendario semanal combinado (todas las salas) o individual
- Solo muestra AVAILABLE
- Click en slot → formulario de reserva (crear clase nueva o asignar borrador)

### Maestro Dependiente
- Calendario semanal solo con SUS clases asignadas (OCCUPIED)
- Click → marcar asistencia, ver alumnos

### Alumno Titular
- Calendario mensual con sus clases + clases de asociados
- Color diferenciado: propias vs asociados
- Click → detalle de clase, estado de pago, cancelar si aplica

### Admin General
- No interactúa con calendarios de salas
- Dashboard con métricas de ocupación global

---

## 6. Endpoints planificados

### Admin Sede
| Method | Endpoint | Description |
|---|---|---|
| PUT | `/api/venues/{id}/schedule` | Configurar/editar horario laboral |
| GET | `/api/venues/{id}/schedule` | Ver horario laboral |
| PUT | `/api/venues/{id}/block-config` | Configurar duración de bloques |
| GET | `/api/venues/{id}/block-config` | Ver configuración de bloques |
| GET | `/api/venues/{id}/rooms/schedule` | Ver calendario de todas las salas |
| GET | `/api/venues/rooms/{id}/schedule` | Ver calendario de una sala |
| POST | `/api/venues/rooms/{id}/maintenance` | Marcar bloque como mantención |
| DELETE | `/api/venues/rooms/maintenance/{id}` | Liberar mantención |

### Maestro
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/rooms/available` | Buscar disponibilidad con filtros |
| POST | `/api/rooms/{id}/book` | Reservar slot |

### Alumno
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me/calendar` | Calendario de mis clases |

---

## 7. Migración desde RoomAvailability

- `RoomAvailability` se elimina al final (Fase 7)
- Los datos de disponibilidad se regeneran desde `RoomScheduleBlock`
- Las clases existentes se vinculan a sus bloques OCCUPIED correspondientes
