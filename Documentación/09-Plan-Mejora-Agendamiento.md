# Plan de Mejora · Sistema de Agendamiento y Reserva de Salas

> **Versión:** 1.1 — Actualizado 19-jul-2026 (auditoría doc vs. código)
> **Complementa:** `07-Sistema-de-Agenda.md` (modelo base de horarios y bloques)
> **Estado real:** ⚠️ este documento seguía rotulado como "pendiente de
> implementación", pero **6 de las 8 fases ya están implementadas** (Fases 1,
> 2, 3, 4, 6, 7 — ver detalle en §7). **Solo la Fase 5 (clase-plantilla
> reutilizable, no consumida al asignar) sigue siendo una brecha real y
> vigente.** La Fase 8 (cierre de código muerto) está parcial. Ver también
> `11-Mejoras-Incorporadas.md` para los PRs que fueron cerrando cada fase.

Este documento captura la visión acordada para el flujo de **descubrimiento y
reserva de salas** del maestro, el **calendario del alumno**, y el modelo de
**clase-plantilla reutilizable**. Es el respaldo del trabajo a realizar.

---

## 1. Visión general

La experiencia debe parecerse a un **sistema de agendamiento**: visualmente
atractivo, fácil de coordinar la semana, basado en un **calendario**.

- Cada **sede** define su propio horario laboral; de ahí se genera la grilla de
  slots por sala.
- El **maestro** busca disponibilidad con filtros, navega sede → sala →
  calendario, y reserva un slot.
- La **clase** es un contenido reutilizable (plantilla) que se asigna a una o
  varias reservas.
- El **alumno** ve sus clases agendadas en un calendario amigable.

---

## 2. Conceptos y separación de responsabilidades

| Concepto | Qué es | Quién lo maneja |
|---|---|---|
| **Horario de sede** | Días + apertura/cierre + duración de bloque + colchón | Sede |
| **Grilla / bloques** (`RoomScheduleBlock`) | Slots generados por sala, con estado `AVAILABLE`/`OCCUPIED`/`MAINTENANCE` | Sistema (se generan del horario) |
| **Reserva de sala** | Un slot tomado (sala + día + hora) | Maestro |
| **Clase (plantilla)** | El "qué": título, disciplina, nivel, capacidad, duración, precio… Reutilizable | Maestro |
| **Instancia de clase** | Copia publicada de una plantilla en una reserva concreta, con sus propias inscripciones | Sistema (al asignar) |

**Regla clave:** la clase es el **"qué"** (reutilizable); la reserva es el
**"cuándo/dónde"**. Una misma plantilla puede asignarse a varias reservas.

---

## 3. Flujo del maestro

1. **Crear Clase** = definir una clase con sus características → queda como
   **plantilla / borrador reutilizable** (NO se pone en una fecha todavía).
2. **Buscar sala** con filtros:
   - **Región** · **Comuna** · **Disciplina** (clickeable) · **Horario** (rango
     de fecha/hora) · **Características de la sala** según disciplina.
3. **Resultados** → lista de **sedes** disponibles que cumplen los filtros.
4. Pinchar **sede** → **salas** de esa sede que cumplen las características.
5. Pinchar **sala** → **calendario** con los slots (disponible / ocupado /
   cerrado / mantención).
6. Pinchar **slot libre** → **crear la reserva** (`bookSlot` → el slot pasa a
   `OCCUPIED`).
7. **Asignar:** tomar una clase ya creada (plantilla) y asignarla a **una o
   varias** reservas. La plantilla **no se consume**: cada asignación crea una
   **instancia independiente** y la plantilla sigue disponible.
   - Editar la plantilla luego afecta solo a las **nuevas** asignaciones; las
     instancias ya creadas quedan como estaban (son una foto del momento).

---

## 4. Calendario del alumno

El alumno ve sus clases agendadas en un calendario amigable, con tres vistas:

- **Propio** — solo sus clases.
- **Por asociado** — el calendario de un beneficiario/asociado específico.
- **Mixto** — todas las clases (propias + asociados) juntas.

---

## 5. Características de sala por tipo (diferenciadores)

Las características diferencian las salas y generan competencia por una mejor
experiencia. Se **agrupan según el tipo** de sala/disciplina:

- **Danza:** espejos, tipo de piso (flotante…), barra de ballet, aire
  acondicionado, calefacción.
- **Música:** amplificación, insonorización, micrófono, entrada auxiliar,
  equipo de grabación, instrumentos (piano / guitarra / batería).
- **Generales:** capacidad, tamaño m², precio/hora.

> Ya existen en el modelo `Room` (`hasMirrors`, `tieneAmplificacion`,
> `tieneInsonorizacion`, `tipoPiso`, etc.). Falta agruparlas por tipo en el
> formulario y exponerlas como filtros del buscador según la disciplina.

---

## 6. Estado actual vs. brechas

### Ya existe (verificado)
- Modelo `VenueSchedule` + `VenueBlockConfig` + `RoomScheduleBlock`.
- Configuración de horario + generación de grilla (funciona E2E).
- Páginas de calendario enrutadas (sede / profesor / alumno) + enlaces en la
  navegación.
- Características de sala en el modelo y en el formulario (lista plana).
- Borradores de clase + endpoint `asignar-reserva` (parcial).

### Brechas a resolver
1. `Venue` solo tiene `city`: faltan **`region`** y **`comuna`** estructurados.
2. `asignarReserva` **consume** el borrador (`DRAFT` → `PUBLISHED`): no permite
   reutilizar la plantilla en varias reservas.
3. El buscador muestra slots directamente: falta la jerarquía **sede → sala →
   calendario**.
4. Las características no están **agrupadas por disciplina** ni expuestas como
   filtros dependientes de la disciplina.
5. `RescheduleService` aún usa el sistema viejo (`RoomAvailability`) con la
   validación "bypassed temporarily": falta migrarlo a `RoomScheduleBlock`.
6. Sede sin horario: debe mostrarse "sin horarios disponibles" + opción de
   **crear/editar horario**.
7. Sistema viejo (`RoomAvailability`, página primitiva `SedeSalaAgenda`, seeder
   deshabilitado) por **eliminar** tras la migración.
8. Calendario del alumno: faltan las vistas **por asociado** y **mixto**.

---

## 7. Plan de trabajo (8 fases) — estado real al 19-jul-2026

### Fase 1 — Modelo de datos ✅ implementada
- `region`/`comuna` en `venues` — presentes en el schema y usados como filtro
  en `BuscarSalasPage.vue`.
- **Clase-plantilla reutilizable:** ⚠️ **NO implementada** (ver Fase 5 — quedó
  documentada aquí en la Fase 1 pero es, en la práctica, la misma brecha que
  la Fase 5 describe con más detalle).

### Fase 2 — Horario de la sede ✅ implementada
- Página de horario funcional; CTA cuando falta configurar.
- `RoomAvailability` (el sistema viejo) **ya no existe** en el repo — no quedan
  referencias en `frontend/src` ni `supabase/`.

### Fase 3 — Características de sala por tipo ✅ implementada
- `BuscarSalasPage.vue` agrupa características por disciplina
  (`caracteristicasPorDisciplina`) y filtra según `filtros.disciplina`.

### Fase 4 — Buscador visual del maestro ✅ implementada
- `BuscarSalasPage.vue` implementa la jerarquía sede → sala → calendario, con
  los filtros descritos.

### Fase 5 — Crear clase (plantilla) + asignar ⚠️ NO implementada — brecha real vigente
- `assign-reserva/index.ts` sigue **consumiendo** el borrador
  (`UPDATE classes SET status='PUBLISHED' ... WHERE id = classId AND status='DRAFT'`
  sobre la **misma fila**) en vez de clonarlo en una instancia nueva. Una clase-
  plantilla no puede reutilizarse en varias reservas hoy — sigue siendo tal
  cual la brecha original de este plan.

### Fase 6 — Reagendar con calendario ✅ implementada
- El mecanismo vigente (`teacher-reschedule-class`, `sede-reschedule-class`,
  ver `02-Reglas-de-Negocio.md` R16.1/R16.2) ya usa `room_schedule_blocks` real,
  con guard atómico y sin bypass. El mecanismo viejo (`propose-reschedule` +
  `teacher-decision`) sigue en el código pero está huérfano de UI (ver R15/R18).

### Fase 7 — Calendario del alumno ✅ implementada (con matiz)
- `MisClasesCalendarioPage.vue` tiene las vistas **propio · por asociado ·
  mixto**. `scheduleService.getUserCalendar()` sigue siendo un stub que lanza
  error 501, pero **el calendario del alumno no depende de él** — usa otra
  fuente de datos real. El stub es código muerto, no una brecha funcional.

### Fase 8 — Cierre ⚠️ parcial
- Se fue eliminando código muerto de forma incremental (ver
  `11-Mejoras-Incorporadas.md` §11.1: se retiró `SedeReagendamientoPage.vue`).
  Pendiente: limpiar el mecanismo huérfano `propose-reschedule`/`teacher-decision`
  si se confirma que no se retomará (ver R15/R17/R18), y resolver la Fase 5.

**Conclusión:** de las 8 fases, **6 están implementadas**, la **Fase 5 sigue
pendiente** (es la única brecha funcional real de este plan) y la **Fase 8 es
un cierre continuo**, no un hito único.
