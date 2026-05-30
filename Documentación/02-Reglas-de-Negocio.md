# Reglas de Negocio · Modo Ensayo

> **Versión:** 2.0 — Actualizado al 30-may-2026
> **Total de reglas:** 18 reglas formales del MVP

Estas reglas son las restricciones e invariantes del sistema, sus mecanismos de aplicación y consecuencias documentadas. Todas están implementadas, validadas en código y/o en la base de datos.

---

## R01 — Pagos condicionados a realización de clase

Todo pago de inscripción queda en estado `RETAINED` (retenido) al momento del cobro y solo se libera (`RELEASED`) cuando la clase asociada pasa a estado `REALIZADA`. Si la clase se confirma como `NO_REALIZADA`, los pagos retenidos pasan a `REFUND_PENDING` para devolución según el método preferido del alumno.

- **Implementación:** Trigger `trg_release_payment` en PostgreSQL + lógica en `ClassConfirmationService`.
- **Validación:** `PaymentServiceTest`, `ClassConfirmationServiceTest`.

---

## R02 — Control de capacidad de clase

No se puede inscribir a un beneficiario en una clase que ha alcanzado su capacidad máxima. La capacidad es definida por el Maestro al crear la clase.

- **Implementación:** Trigger `trg_check_capacity` + `CHECK` al insertar en `enrollments`.
- **Excepción:** El sistema responde con error de negocio "La clase está llena".

---

## R03 — Auditoría de estados de clase

Cada transición de estado de una clase (`BORRADOR`, `PUBLISHED`, `IN_PROGRESS`, `COMPLETED`, `POR_VALIDAR`, `CANCELLED`, `REALIZADA`, `NO_REALIZADA`) debe quedar registrada en `class_status_history` con el usuario que realizó el cambio y la fecha.

- **Implementación:** Trigger `trg_class_status_change`.

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

Un único pago en MercadoPago genera un único `consolidated_payment` y N `payment_items` que se distribuyen entre las clases del carrito. Cada `payment_item` queda en estado `RETAINED` hasta que su clase se confirma.

- **Implementación:** `PaymentService.createMercadoPagoPreference()` + webhook handler.

---

## R13 — Liberación o devolución por confirmación de clase

Cuando el Admin de Sede confirma una clase como:
- **`REALIZADA`** → todos los `payment_items` asociados pasan a `RELEASED`. El Maestro recibe el pago.
- **`NO_REALIZADA`** → todos los `payment_items` pasan a `REFUND_PENDING`. Los alumnos reciben sus devoluciones.

Esta confirmación es la **única forma** de liberar pagos.

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

- **Implementación:** `RescheduleTimeoutProcessor` (Spring `@Scheduled` cada hora) + `@EnableScheduling`.

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
        [clase REALIZADA] → RELEASED (trigger automático)
                    ↓
        [clase NO_REALIZADA] → REFUND_PENDING → REFUNDED (manual por admin)
```

### Clase

```
BORRADOR → PUBLISHED → IN_PROGRESS → POR_VALIDAR ───┬─→ REALIZADA
                                                     └─→ NO_REALIZADA
                ↓                                    ↓
              CANCELLED                         (también CANCELLED)
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
| R01 | `ClassConfirmationService.java` | `ClassConfirmationServiceTest.java` |
| R02 | Trigger `trg_check_capacity` (03_procedures.sql) | Manual + integration |
| R03 | Trigger `trg_class_status_change` (03_procedures.sql) | Manual |
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
