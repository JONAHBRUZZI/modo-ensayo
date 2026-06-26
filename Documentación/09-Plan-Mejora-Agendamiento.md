# Plan de Mejora · Sistema de Agendamiento y Reserva de Salas

> **Versión:** 1.0 — 18-jun-2026
> **Complementa:** `07-Sistema-de-Agenda.md` (modelo base de horarios y bloques)
> **Estado:** especificación aprobada, pendiente de implementación

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

## 7. Plan de trabajo (8 fases)

### Fase 1 — Modelo de datos
- Agregar `region` y `comuna` a `Venue` (entidad + columnas + formulario de
  registro de sede + visible en admin y búsqueda). Lista estándar de regiones y
  comunas de Chile.
- **Clase-plantilla reutilizable:** la clase creada queda como plantilla
  permanente (`DRAFT`); asignarla a una reserva **clona** una instancia
  (`PUBLISHED` con sala + horario), dejando la plantilla intacta.

### Fase 2 — Horario de la sede
- Pulir la página de horario: acceso claro, botón **crear/editar horario**, y
  CTA cuando la sede no tiene horario.
- Migrar y limpiar el sistema viejo (`RoomAvailability`, `SedeSalaAgenda`
  primitiva, seeder) → todo a `RoomScheduleBlock`.

### Fase 3 — Características de sala por tipo
- Agrupar/mostrar características según tipo (Danza vs Música) en el formulario
  de sala, según la documentación.

### Fase 4 — Buscador visual del maestro
- Filtros: región · comuna · disciplina · horario (rango) · características según
  disciplina.
- Resultados → sedes → salas → calendario → pinchar slot libre → **reserva**
  (`bookSlot`).

### Fase 5 — Crear clase (plantilla) + asignar
- "Crear Clase" del maestro = crear plantilla reutilizable.
- "Mis reservas" → asignar una clase creada a una o varias reservas (reuso).
- Migrar `asignarReserva`: clonar plantilla → instancia, sin consumir el
  borrador.

### Fase 6 — Reagendar con calendario
- Reagendar = pinchar sala → calendario tomado/libre → elegir slot.
- Migrar `RescheduleService` a `RoomScheduleBlock` (quitar el bypass temporal).

### Fase 7 — Calendario del alumno
- `MisClasesCalendario`: vistas **propio · por asociado · mixto**.
- Verificar/implementar `GET /users/me/calendar`.

### Fase 8 — Cierre
- Eliminar código muerto del sistema viejo.
- Verificación E2E de cada flujo + ajustar tests.

**Orden de construcción:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8. Cada fase deja algo
usable y verificable.
