# Reglas de Negocio · Modo Ensayo

> **Versión:** 2.3 — Actualizado al 16-ago-2026
> **Total de reglas:** 19 reglas formales del MVP

Estas reglas son las restricciones e invariantes del sistema, sus mecanismos de aplicación y consecuencias documentadas. La mayoría están implementadas y validadas en código y/o en la base de datos; R19 está implementada en código, pendiente de desplegar en producción (ver nota en la regla).

---

## R01 — Pagos condicionados a realización de clase

Todo pago de inscripción queda en estado `RETAINED` (retenido) al momento del cobro y solo se libera (`RELEASED`) cuando la clase asociada se confirma como realizada (estado `COMPLETED`). Si la clase se confirma como no realizada (estado `SUSPENDED`), los pagos retenidos pasan a `REFUND_PENDING` para devolución según el método preferido del alumno.

- **Implementación:** Edge Function `confirm-class` (Deno/TypeScript, service-role). No hay trigger de liberación; la lógica vive en la función. El desembolso real al profesor queda registrado en `teacher_payouts` (estado `PENDING`).
- **Validación:** manual + verificación en SQL Editor (`payments.status`, `teacher_payouts`).

---

## R02 — Control de capacidad de clase

No se puede inscribir a un beneficiario en una clase que ha alcanzado su capacidad máxima. La capacidad se toma del tope de la sala (`rooms.capacity`) al crear o publicar la clase.

- **Implementación:** validación de cupo en las Edge Functions `create-class` (fija `capacity` desde la sala) y `mercadopago-webhook` (cuenta inscripciones activas contra `capacity` antes de confirmar). La inscripción única por beneficiario la garantiza el índice `enrollments_unique_beneficiary` sobre `(class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))`.
- **Excepción:** El sistema responde con error de negocio "La clase está llena".

---

## R03 — Auditoría de estados de clase

Cada transición de estado de una clase debe quedar registrada en `class_status_history`. Los estados reales del enum `class_status` son: `DRAFT`, `PUBLISHED`, `IN_PROGRESS`, `FULL`, `CANCELLED`, `COMPLETED`, `SUSPENDED`, `POR_VALIDAR`. (`COMPLETED` = clase realizada; `SUSPENDED` = no realizada; `POR_VALIDAR` = a la espera de confirmación de la sede; `FULL` = cupo lleno).

- **Implementación:** Trigger `trg_classes_status` → función `track_class_status()`, que inserta la transición en `class_status_history`.

---

## R04 — Verificación de identidad obligatoria

Cualquier usuario que quiera operar como **Maestro** o **Administrador de Sede** debe pasar previamente por verificación de identidad: subir documento (RUT u otra cédula), revisión manual por Admin General y aprobación con estado `APROBADO`.

- **Estados:** `SIN_VALIDAR` → `PENDING` → `APPROVED` o `REJECTED`.
- **Implementación:** subida del documento vía `userService.uploadIdentity()` (frontend) → fila en `identity_verifications`. Revisión manual del Admin General vía `adminService.reviewIdentity()` (`frontend/src/services/adminService.js`): actualiza `identity_verifications.status` y `profiles.identidad_validada`/`identidad_estado` directo por PostgREST, protegido por policy RLS admin-only (no es una Edge Function).
- **Una sola vez:** Una vez aprobada, la identidad es permanente y vale para todos los contextos.

---

## R05 — Unicidad de documento de identidad

Un mismo documento de identidad no puede estar `APROBADO` en más de una cuenta de usuario. Esto previene suplantación y duplicación de cuentas validadas.

- **✅ Estado real: implementado (dos capas).** El backend Spring Boot original tenía este chequeo
  como constraint duro
  (`IdentityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot()`) y se perdió en la
  migración a Supabase; quedó cerrado el 16-ago. El 11-jul se había agregado un aviso en el frontend
  (RPC
  `rut_ya_registrado`, `SECURITY DEFINER`, migración `20260711000000_rut_exists_rpc.sql`): al
  ingresar el RUT en la verificación de identidad, consulta si ya existe (en
  `identity_verifications` PENDING/APPROVED o en `profiles.rut` de otra cuenta) y **bloquea el
  envío del formulario** con un aviso. Es un chequeo del lado del cliente en el momento de subir el
  documento — no cubre aprobaciones vía API directa ni condiciones de carrera por sí solo.
- **Constraint en base de datos (16-ago):** índice único parcial
  `identity_verifications_document_approved_unique` sobre `document_number` normalizado (mismo
  formato que `rut_ya_registrado`: sin puntos/guiones/espacios, mayúsculas), condicionado a
  `status = 'APPROVED'` (migración `20260816000000_identity_document_unique.sql`). Cierra el gap
  que el aviso del frontend no cubría. `adminService.reviewIdentity()` captura la violación
  (código Postgres `23505`) y devuelve un mensaje claro en vez del error crudo de la base de datos.
  **Estado: aplicado en producción (16-ago)** — sin documentos duplicados preexistentes, el índice
  se creó sin conflictos.

---

## R06 — Aprobación de sedes

Las sedes registradas por un Administrador de Sede deben ser aprobadas por un Admin General antes de ser visibles públicamente y poder recibir reservas.

- **Estados:** `PENDIENTE` → `APROBADA` o `RECHAZADA`.

---

## R07 — Beneficiarios y carrito familiar (asociados)

Un usuario Alumno puede inscribir a sus **asociados** (familiares o dependientes) en cualquier clase. El pago siempre lo realiza el usuario titular. Los beneficiarios se identifican por tipo (`USER` o `ASSOCIATE`) y su ID correspondiente.

- **Carrito consolidado:** Un solo pago puede inscribir a 3+ personas distintas en clases distintas.

---

## R08 — Roles dinámicos por acción

Los roles se asignan automáticamente al sistema según las acciones del usuario, no según declaración:
- Rol `TEACHER` se asigna automáticamente al confirmar la primera **reserva de sala** (no al crear borrador).
- Rol `VENUE_ADMIN` se asigna al **aprobarse la primera sede** del usuario.
- Rol `ADMIN` se asigna manualmente por otro Admin General.

Una vez adquirido, un rol es **permanente** (no se pierde aunque el usuario deje de tener actividades).

- **Implementación:** asignación/revocación manual de rol vía Edge Function `admin-users` (`action: assignRole` / `revokeRole`, solo admin, service role).

---

## R09 — Método de devolución preferido

Cada usuario que reciba pagos retenidos a su favor (alumno cuando se cancela una clase) debe tener configurado al menos un método de devolución (transferencia bancaria, billetera digital). El sistema usa el método **preferido** del alumno al procesar reembolsos.

---

## R10 — Unicidad de inscripción

Un mismo beneficiario no puede inscribirse dos veces en la misma clase, ni siquiera mediante carritos separados.

- **Implementación:** `UNIQUE (class_id, beneficiary_type, beneficiary_id)` en tabla `enrollments`.

---

## R11 — Atomicidad del checkout

El proceso de checkout debe ser atómico: o se inscriben **todos** los items del carrito y se crean todos los pagos, o no se hace ninguna inscripción y se devuelve el dinero al carrito. No puede quedar estado parcial.

- **Implementación:** Edge Functions `mercadopago-create-preference` (crea la preferencia y la `payment_session`) + `mercadopago-webhook` (al aprobarse el pago, crea todas las `enrollments`/`payments` del carrito de una vez). No hay transacción ACID explícita como en Spring; la atomicidad depende de que el webhook procese el carrito completo en una sola invocación.

---

## R12 — Pago consolidado distribuido

Un único pago en MercadoPago genera una sesión de pago (`payment_sessions`, una por checkout) y N filas en `payments` (una por inscripción), que se distribuyen entre las clases del carrito. Cada fila de `payments` queda en estado `RETAINED` hasta que su clase se confirma. (El modelo heredado del backend Spring `consolidated_payments` / `payment_items` ya no existe.)

- **Implementación:** Edge Functions `mercadopago-create-preference` + `mercadopago-webhook`.

---

## R13 — Liberación o devolución por confirmación de clase

Cuando el Admin de Sede confirma una clase como:
- **`COMPLETED`** (realizada) → todas las filas de `payments` asociadas pasan a `RELEASED`, se crea el `teacher_payouts` (PENDING) y se **liberan los bloques** de horario (`room_schedule_blocks` OCCUPIED → AVAILABLE).
- **`SUSPENDED`** (no realizada) → **NO se reembolsa de inmediato**: se abre una **ventana de reagendamiento de 24h** (`classes.reschedule_deadline`); los `payments` quedan `RETAINED` y las `enrollments` `ACTIVE`, se liberan los bloques del horario viejo y se notifica al responsable (ver R16). El reembolso es **diferido**: si nadie reagenda dentro del plazo, el cron `process_class_reschedule_timeouts` (cada hora) pasa los pagos a `REFUND_PENDING`, cancela las inscripciones y notifica.

Esta confirmación (Edge Function `confirm-class`) es la **única forma** de liberar pagos.

**Reembolso (`process-refunds`, cron cada 10 min):** el reembolso se hace por
**MercadoPago** (canal del pago original) y es **parcial**, por el monto exacto
de cada inscripción (`payments.amount`) — un mismo pago de MP puede cubrir varias
clases del carrito, así que se devuelve solo lo de la clase afectada, no el total.
Usa `X-Idempotency-Key` por `payment.id`. Un error permanente de MP (4xx) marca el
pago `FAILED` para atención manual; los transitorios (5xx/429) se reintentan.

**Desembolso al profesor (`process-payouts`, cron cada 15 min):** `disburseToSeller` (MP) fue
reemplazado por `PayoutProvider.sendPayout()` implementado con **Fintoc** (transferencia directa a
los datos bancarios de `refund_methods`), no con MercadoPago — MP no tiene API de money-out
(confirmado). Stripe Connect se descartó: exige entidad legal fuera de Chile. **Estado (16-ago):
código escrito y desplegable, sin probar contra la API real** — falta que el usuario genere el par
de llaves JWS que exige Fintoc para firmar transferencias (aparte de la Secret Key de la cuenta) y
haga una prueba con un monto mínimo. Ver `11-Mejoras-Incorporadas.md` §15.

---

## R14 — Confirmación explícita en decisiones irreversibles

Toda decisión irreversible o de alto impacto requiere confirmación explícita del usuario (formato "¿Confirma su acción? Sí / No"). Aplica especialmente a:
- Pago en el carrito
- Cancelación de inscripción (con aviso de reembolso manual)
- Confirmación de clase realizada/no realizada
- Decisión del Maestro en reagendamiento (aceptar/rechazar fecha propuesta)
- Decisión del Alumno en reagendamiento (aceptar/rechazar)

- **Implementación:** Componente `ConfirmModal.vue` + validación de campo `confirmacion: true` en backend.

---

## R15 — Sugerencia de fechas según agenda real

Al iniciar un reagendamiento, el sistema sugiere fechas alternativas calculadas sobre la **agenda real de la sala** (no horarios genéricos). Solo se ofrecen slots libres en sala y horarios donde el Maestro no tiene otra clase.

- **Implementación:** Edge Function `propose-reschedule`, que consulta la disponibilidad real de `room_schedule_blocks` de la sala y las clases del profesor antes de sugerir horarios.

---

## R16 — Timeout de 48 horas para decisión del Alumno

Cuando el Maestro acepta un reagendamiento, los alumnos inscritos reciben una notificación con un contador de **48 horas** para aceptar o rechazar. Si no responden dentro del plazo:
- Su silencio se interpreta como **rechazo** automático.
- El pago pasa a `REFUND_PENDING` según método preferido.

- **Implementación:** Edge Function `student-decision` (aceptar/rechazar del alumno; el rechazo pasa su `payments` a REFUND_PENDING) + la vista `AlumnoReagendamientoPage` (`/alumno/reagendamiento/:id`) con banner en "Mis Clases" y notificación accionable. El vencimiento a las 48 h lo procesa un job `pg_cron` que marca las respuestas sin responder como `TIMEOUT`.

---

## R16.1 — Reagendamiento de una clase NO realizada (ventana de 24h, reembolso diferido)

Cuando la sede marca una clase como **no realizada**, en vez de reembolsar de inmediato se abre una ventana de **24h** (`classes.reschedule_deadline`) para reagendarla. **Reloj único**: corre desde la marca de "no realizada"; la responsabilidad de avisar a tiempo es del responsable.

- **Clase `PROPIA` (profesor independiente):** `confirm-class` lo notifica (`CLASS_RESCHEDULE_OFFERED`). En `/profesor/reagendamientos` (con cuenta regresiva) pone un **motivo obligatorio** y **paga un arriendo nuevo** de sala (arriendo normal con split a la sede) usando la clase caída como `borradorId`. Al aprobarse el pago, el webhook republica la clase en el nuevo horario y dispara la decisión de los alumnos.
- **Clase `ASIGNADA` (creada por la sede):** la sede reagenda desde `/sede/reagendar/:classId` eligiendo una sala **propia** (sin pago), con motivo obligatorio; la Edge Function `sede-reschedule-class` republica la clase, dispara la decisión de los alumnos y **notifica al profesor dependiente** (solo aviso).
- En ambos casos, los alumnos **aceptan o rechazan** la nueva fecha (reusa R16 / `student-decision`, 48h; el que rechaza recibe reembolso).
- **Vencimiento:** si nadie reagenda en 24h, el cron `process_class_reschedule_timeouts` reembolsa (RETAINED → REFUND_PENDING), cancela inscripciones y cierra la ventana.

**Decisiones fijadas:** independiente **paga** el arriendo nuevo; sede **no paga** (usa su sala). *Fuera de alcance (futuro):* reglas de plazo 7d/72h para cobro parcial del reagendamiento.

---

## R17 — Reagendamiento masivo si Maestro rechaza

Si el Maestro rechaza la propuesta de reagendamiento o no responde:
- La clase queda en estado `CANCELLED`.
- **Todos** los pagos asociados pasan a `REFUND_PENDING` masivamente.
- Todos los alumnos reciben notificación de devolución.

---

## R18 — Quién decide el reagendamiento según tipo de clase

La decisión sobre un reagendamiento depende del `tipoClase`:

| tipoClase | Quién decide |
|---|---|
| `PROPIA` (clase del Maestro Independiente en su propia sala) | Solo el Maestro dueño de la clase |
| `ASIGNADA` (clase creada por la Sede y asignada a un Maestro Dependiente) | Solo el Admin General de la Sede |

Si la decisión la intenta el actor incorrecto, el sistema rechaza con error 403.

- **Implementación:** Validación en la Edge Function `teacher-decision` (chequea `classes.tipo_clase` y que el actor sea el `teacher_id` correcto); para clases `ASIGNADA` la decisión la toma la sede vía `sede-reschedule-class`.

---

## R19 — Cancelación y reembolso de arriendo de sala (decisión de producto, 16-ago-2026)

Decisión tomada para cerrar el gap de "reembolso de arriendos de sala" (ver
`15-Roadmap-y-Pendientes.md`, deuda técnica):

- **Quién puede cancelar:** el **profesor** que pagó el arriendo, o la **sede** dueña de la sala.
  Cualquiera de los dos puede iniciar la cancelación.
- **Plazo:** hasta **24 horas antes** del horario reservado (`room_schedule_blocks.start_time`).
  Pasado ese plazo, no se puede cancelar por esta vía.
- **Reembolso:** **total** — se devuelve el 100% de lo pagado (incluida la comisión de la
  plataforma), sin penalidad.
- **Guarda adicional (criterio de implementación, no pedida explícitamente):** no se puede cancelar
  si la clase ya tiene inscripciones `ACTIVE` — ese caso lo cubre el flujo existente de "clase no
  realizada" (R13/R16.1).

- **Implementación (16-ago):** Edge Function `cancel-room-reservation`. Reembolsa con el token de la
  **sede** (no la plataforma, porque el arriendo se cobró con split) contra la API de MercadoPago;
  libera `room_schedule_blocks` a `AVAILABLE` y pasa la clase a `CANCELLED`. Columna
  `classes.payment_session_id` (migración `20260816020000_room_reservation_cancel.sql`) ubica el
  pago a reembolsar. **Aplicado en producción (16-ago)** — migración y despliegue de
  `cancel-room-reservation`/`mercadopago-webhook` confirmados. **Sin probar con un reembolso real
  todavía** — ver `11-Mejoras-Incorporadas.md` §14.

---

## Flujo de Estados (resumen)

### Pago

```
[Usuario paga] → RETAINED
                    ↓
        [clase COMPLETED] → RELEASED (Edge Function confirm-class)
                    ↓
        [clase SUSPENDED] → REFUND_PENDING → REFUNDED (process-refunds / manual)
```

### Clase

```
DRAFT → PUBLISHED → IN_PROGRESS → POR_VALIDAR ───┬─→ COMPLETED  (realizada)
                                                  └─→ SUSPENDED  (no realizada)
              ↓             ↓
          CANCELLED       FULL (cupo lleno)
```

### Reagendamiento

```
PROPUESTO → DECISION_MAESTRO ─── acepta ──→ NOTIFICADO_ALUMNOS
                                                ↓
                  rechaza ────→ CANCELLED      [contador 48h]
                                                ↓
                                  ┌─ acepta → REAGENDADO
                                  ├─ rechaza → DEVOLUCION_INDIVIDUAL
                                  └─ timeout → DEVOLUCION_INDIVIDUAL
```

---

## Trazabilidad de implementación

| Regla | Archivo de implementación | Test |
|---|---|---|
| R01 | Edge Function `confirm-class` | Manual + SQL Editor |
| R02 | Edge Functions `create-class` / `mercadopago-webhook` + índice `enrollments_unique_beneficiary` | Manual + integration |
| R03 | Trigger `trg_classes_status` → `track_class_status()` | Manual |
| R04 | `userService.uploadIdentity()` (frontend) + `adminService.reviewIdentity()` (PostgREST + RLS admin) | Manual |
| R05 | Índice único `identity_verifications_document_approved_unique` (BD) + RPC `rut_ya_registrado` (aviso frontend) | Manual |
| R06 | Edge Function `admin-approve-venue` | Manual |
| R07 | `associateService.js`, `cart_items.beneficiary_type`/`beneficiary_id` | Manual |
| R08 | Edge Function `admin-users` (`assignRole`/`revokeRole`) | Manual |
| R09 | `userService.js` (CRUD sobre `refund_methods`) | Manual |
| R10 | Índice único `enrollments_unique_beneficiary` en `enrollments` | BD |
| R11 | Edge Functions `mercadopago-create-preference` + `mercadopago-webhook` | Manual (sandbox) |
| R12 | Edge Functions `mercadopago-create-preference` + `mercadopago-webhook` | Sandbox |
| R13 | Edge Function `confirm-class` | Manual + SQL Editor |
| R14 | `ConfirmModal.vue` + validación `confirmacion` | `CartPage.test.js` (frontend, Vitest) |
| R15 | Edge Function `propose-reschedule` | Manual |
| R16 | Edge Function `student-decision` + job `pg_cron` (timeout 48h) | Manual |
| R17 | Edge Function `teacher-decision` (rechazo → `REFUND_PENDING` masivo) | Manual |
| R18 | Edge Functions `teacher-decision` / `sede-reschedule-class` según `tipo_clase` | Manual |
