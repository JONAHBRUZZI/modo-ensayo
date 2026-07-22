# Sistema de Agenda y Calendario · Modo Ensayo

> **Versión:** 1.1 — Actualizado 19-jul-2026 (auditoría doc vs. código)
> **Reemplaza:** `RoomAvailability` (entidad obsoleta, migración completada — ver §7)
>
> ⚠️ La **sección 6 (endpoints planificados)** describe una API REST estilo
> Spring que nunca se implementó así — se reemplaza por Edge Functions +
> PostgREST directo (ver la versión corregida de esa sección).

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
| status | `block_status` (enum) | `AVAILABLE`, **`HELD`**, `OCCUPIED`, `MAINTENANCE` — `HELD` se agregó el 24-jun-2026 (`20260624000001_block_status_held.sql`), no estaba en la versión 1.0 de este doc |
| class_id | UUID | **sin `REFERENCES` real** (relación "blanda"; nullable, se usa cuando `OCCUPIED`) |
| held_until | TIMESTAMPTZ | Solo si `HELD`: hasta cuándo está reservado temporalmente mientras se paga el arriendo |
| held_by | UUID | FK → auth.users(id); quién inició el pago que sostiene el `HELD` |
| UNIQUE | (room_id, start_time) | `uq_rsb_room_start` — permite que la regeneración use `ON CONFLICT DO NOTHING` real (ver §7) |

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
AVAILABLE ──┬──→ HELD (arriendo de sala: pago iniciado, ~15 min)
            ├──→ OCCUPIED (al agendar clase directo, o al pagarse un HELD)
            ├──→ MAINTENANCE (admin marca mantención)
            └── (se mantiene)

HELD ────────┬──→ OCCUPIED (pago aprobado)
             └──→ AVAILABLE (cron release_expired_holds, held_until vencido)

OCCUPIED ────→ AVAILABLE (al cancelar clase o reagendar)

MAINTENANCE ──→ AVAILABLE (admin libera)
```

`HELD` es el estado que sostiene un bloque mientras el profesor paga el arriendo
de una sala (`reserve-room-preference`, MercadoPago Connect) — sin él, dos
profesores podrían pagar por el mismo horario. Un cron cada 5 min
(`release_expired_holds`) libera los `HELD` cuyo pago no se completó.

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
- Buscador con filtros checkboxes (disciplina, características de sala) —
  `BuscarSalasPage.vue`
- Calendario semanal combinado (todas las salas) o individual
- Solo muestra AVAILABLE
- Click en slot → **implica un pago real**: `reserve-room-preference` marca los
  bloques elegidos como `HELD`, crea la preferencia de MercadoPago con split a
  la sede (comisión configurable) y redirige al Checkout. Al aprobarse el pago,
  el webhook los pasa a `OCCUPIED` y publica/crea la clase. Si no paga, el cron
  `release_expired_holds` los libera. (El método `scheduleService.bookSlot()`
  sigue existiendo en el código pero **ninguna vista lo invoca**; el flujo real
  y único es el de pago vía `reserve-room-preference`.)

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

## 6. Acceso real (Supabase, no REST propio)

No existe una API REST `/api/...` — el acceso es PostgREST directo (sujeto a
RLS) o Edge Functions para lo privilegiado. Superficie real (ver también
`06-API-Endpoints.md`):

| Acceso | Vía | Uso |
|---|---|---|
| Ver/editar horario laboral | `supabase.from('venue_schedules')` (PostgREST) | Admin de sede |
| Ver/editar config. de bloques | `supabase.from('venue_block_configs')` | Admin de sede |
| Ver calendario de una sala | `scheduleService.getRoomSchedule(roomId, from, to)` → `room_schedule_blocks` | Todos los roles (RLS filtra `AVAILABLE` para público) |
| Marcar/liberar mantención | `scheduleService.markMaintenance()` / `releaseMaintenance()` → `room_maintenances` + `room_schedule_blocks` | Admin de sede |
| Regenerar bloques manualmente | Edge Function `generate-blocks` → RPC `regenerate_schedule_blocks()` | Admin de sede / Admin |
| Reservar sala (con pago) | Edge Function `reserve-room-preference` | Profesor (arriendo) |
| Reservar sala (sin pago, uso interno) | Edge Function `book-slot` | Profesor/sede — sin caller real en el frontend hoy |
| Calendario de mis clases (alumno) | `MisClasesCalendarioPage.vue` (fuente propia, no depende de `scheduleService.getUserCalendar()`) | Alumno. `getUserCalendar()` sigue siendo un stub que lanza error 501, pero ya no es la fuente real del calendario |

---

## 7. Migración desde RoomAvailability — completada

- `RoomAvailability` **ya fue eliminado**: no queda ninguna referencia en el
  repo (`frontend/src` ni `supabase/`). Esta migración terminó (ver también
  `09-Plan-Mejora-Agendamiento.md`, Fase 2).
- Los datos de disponibilidad se generan/regeneran desde `room_schedule_blocks`
  (`regenerate_schedule_blocks()`, con `UNIQUE(room_id, start_time)` real desde
  el 23-jun-2026 — antes de eso el `ON CONFLICT` no tenía constraint que lo
  respaldara).
- Las clases se vinculan a sus bloques `OCCUPIED` mediante `class_id` (relación
  blanda, sin FK declarada en la BD).
