# Reglas de Negocio · Modo Ensayo

> **Versión:** 2.1 — Actualizado al 07-jul-2026
> **Total de reglas:** 18 reglas formales del MVP

Estas reglas son las restricciones e invariantes del sistema, sus mecanismos de aplicación y consecuencias documentadas. Todas están implementadas, validadas en código y/o en la base de datos.

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
- **Implementación:** Validación en `UserService.uploadIdentity()` y `ClassService.createClassInternal()`.
- **Una sola vez:** Una vez aprobada, la identidad es permanente y vale para todos los contextos.

---

## R05 — Unicidad de documento de identidad

Un mismo documento de identidad no puede estar `APROBADO` en más de una cuenta de usuario. Esto previene suplantación y duplicación de cuentas validadas.

- **Implementación:** `IdentityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot()`.

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

- **Implementación:** `PaymentService.checkout()` en transacción Spring `@Transactional`.

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

**Desembolso al profesor (`process-payouts`, cron cada 15 min):** el giro real
(`disburseToSeller`) es un **stub de Fase 0** (money-out MercadoPago Chile
pendiente): el `teacher_payouts` queda registrado pero el dinero aún no se gira.

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

- **Implementación:** `RescheduleService.sugerirFechas()` con consulta a `class` + `room_availability`.

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

## R16.2 — Reagendamiento de una clase PUBLICADA (antes de que ocurra)

Distinto de R16.1 (clase ya caída), el **profesor** puede mover una clase suya que **todavía no ocurre** (`PUBLISHED` o `FULL`, con `start_time` futuro) desde `/profesor/clases/:claseId/reagendamiento`.

- Solo a otro horario de la **misma sala** que ya arrienda → **no hay pago** (cambiar de sala implicaría un arriendo nuevo; queda fuera de alcance).
- Motivo **obligatorio**; se muestran los bloques `AVAILABLE` reales de esa sala (30 días).
- La Edge Function `teacher-reschedule-class` toma los bloques nuevos con guard atómico (si otro los tomó → 409 y la clase queda intacta), **libera los bloques del horario viejo** y actualiza `start_time`/`end_time`/`duration`. La sala y el cupo no cambian.
- Los alumnos inscritos **aceptan o rechazan** la nueva fecha (reusa R16 / `student-decision`, 48h; el que rechaza recibe reembolso).

*Fuera de alcance (futuro):* antelación mínima para reagendar y tope de reagendamientos por clase.

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

- **Implementación:** Validación en `RescheduleService.teacherDecision()` con chequeo de `Class.tipoClase`.

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
| R04 | `UserService.uploadIdentity()`, `ClassService.createBorrador()` | Integration |
| R05 | `IdentityVerificationRepository` | Manual |
| R06 | `AdminController.aprobarSede()` | Manual |
| R07 | `AssociateController`, `CartItem.beneficiaryType` | Integration |
| R08 | `ClassService.asignarRolTeacher()` | Integration |
| R09 | `RefundMethodService` | Manual |
| R10 | Constraint UNIQUE en `enrollments` | BD |
| R11 | `PaymentService.checkout() @Transactional` | `PaymentServiceTest.java` |
| R12 | `PaymentService.createMercadoPagoPreference()` | Sandbox |
| R13 | `ClassConfirmationService` | `ClassConfirmationServiceTest.java` |
| R14 | `ConfirmModal.vue` + validación `confirmacion` | `CartPage.test.js` |
| R15 | `RescheduleService.sugerirFechas()` | `RescheduleServiceTest.java` |
| R16 | `RescheduleTimeoutProcessor @Scheduled` | `RescheduleServiceTest.java` |
| R17 | `RescheduleService.teacherDecision(rechazo)` | `RescheduleServiceTest.java` |
| R18 | `RescheduleService.teacherDecision()` validación actor | `RescheduleServiceTest.java` |
