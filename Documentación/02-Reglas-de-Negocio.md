# Reglas de Negocio · Modo Ensayo

> **Versión:** 3.0 — Actualizado al 19-jul-2026 (auditoría doc vs. código)
> **Total de reglas:** 21 reglas formales del MVP (18 originales + R04.1, R04.2, R16.2)

Estas reglas son las restricciones e invariantes del sistema, sus mecanismos de aplicación y consecuencias documentadas. La columna/sección de implementación de cada regla apunta al código **real y vigente** (Edge Functions Deno, migraciones SQL o componentes Vue) — el backend Spring Boot mencionado en versiones anteriores de este documento **ya no existe** en el repositorio (ver `CLAUDE.md`).

> ⚠️ **R15, R17 y R18 describen un mecanismo de reagendamiento que sigue en el código (`propose-reschedule` + `teacher-decision`) pero que ya no tiene ningún punto de entrada en el frontend** desde que se retiró la última vista que lo invocaba (`SedeReagendamientoPage.vue`, PR #53, 19-jul-2026). El mecanismo vigente y usado hoy es el de R16/R16.1/R16.2 (`teacher-reschedule-class`, `sede-reschedule-class`, `student-decision`). Se documentan igual por precisión (el código sigue desplegado y es alcanzable vía API directa), pero no reflejan ningún flujo real accesible desde la UI.

---

## R01 — Pagos condicionados a realización de clase

Todo pago de inscripción queda en estado `RETAINED` (retenido) al momento del cobro y solo se libera (`RELEASED`) cuando la clase asociada se confirma como realizada (estado `COMPLETED`). Si la clase se confirma como no realizada (estado `SUSPENDED`), los pagos retenidos pasan a `REFUND_PENDING` para devolución según el método preferido del alumno.

- **Implementación:** Edge Function `confirm-class` (Deno/TypeScript, service-role). No hay trigger de liberación; la lógica vive en la función. El desembolso real al profesor queda registrado en `teacher_payouts` (estado `PENDING`).
- **Validación:** manual + verificación en SQL Editor (`payments.status`, `teacher_payouts`).

---

## R02 — Control de capacidad de clase

No se puede inscribir a un beneficiario en una clase que ha alcanzado su capacidad máxima. La capacidad se toma del tope de la sala (`rooms.capacity`) al crear o publicar la clase.

- **Implementación (aplicación):** `create-class/index.ts:142-150` fija `capacity` desde `rooms.capacity` (ignora lo que venga del formulario); `mercadopago-webhook/index.ts` cuenta inscripciones activas contra `capacity` antes de confirmar cada ítem del carrito (fast-fail de UX).
- **Implementación (autoridad real, a nivel de BD):** trigger `trg_enforce_class_capacity` → función `enforce_class_capacity()` (`supabase/migrations/20260707120000_enforce_class_capacity.sql`). Antes de cada `INSERT` en `enrollments` hace `SELECT ... FOR UPDATE` sobre la fila de `classes`, cerrando la condición de carrera que el chequeo de aplicación (dos pasos, no atómico) no puede evitar: dos pagos concurrentes ya no pueden sobrevender el último cupo.
- **Inscripción única por beneficiario:** índice `enrollments_unique_beneficiary` sobre `(class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))` (ver R10).
- **Excepción:** El sistema responde con error de negocio "La clase está llena".

---

## R03 — Auditoría de estados de clase

Cada transición de estado de una clase debe quedar registrada en `class_status_history`. Los estados reales del enum `class_status` son: `DRAFT`, `PUBLISHED`, `IN_PROGRESS`, `FULL`, `CANCELLED`, `COMPLETED`, `SUSPENDED`, `POR_VALIDAR`. (`COMPLETED` = clase realizada; `SUSPENDED` = no realizada; `POR_VALIDAR` = a la espera de confirmación de la sede; `FULL` = cupo lleno).

- **Implementación:** Trigger `trg_classes_status` → función `track_class_status()`, que inserta la transición en `class_status_history`.

---

## R04 — Verificación de identidad obligatoria

Un usuario que ya tiene el rol **Maestro** (`TEACHER`) debe tener su identidad validada para publicar una clase `PROPIA` (no borrador): subir documento (RUT u otra cédula), revisión manual por Admin General y aprobación con estado `APROBADO`.

> ⚠️ **Matiz importante:** el chequeo solo se aplica si el usuario **ya tiene** el rol `TEACHER` (`create-class/index.ts:71-77`: `if (!esAsignada && roles.includes("TEACHER"))`). Un usuario que publica su **primera** clase (sin el rol aún) puede hacerlo **sin** identidad validada — el rol recién se asigna automáticamente al publicar (ver R08). No se exige a las clases `ASIGNADA` (las crea la sede).

- **Estados:** `SIN_VALIDAR` → `PENDING` → `APPROVED` o `REJECTED` (columna `profiles.identidad_estado` / `identity_verifications.status`).
- **Implementación:** `create-class/index.ts:71-77` (chequeo al publicar); subida de documento en `frontend/src/views/IdentityUploadPage.vue` → `userService.uploadIdentityDocument()`.
- **Una sola vez:** Una vez aprobada, la identidad es permanente y vale para todos los contextos.

---

## R04.1 — Perfil profesional completo obligatorio (para publicar clase `PROPIA`)

Antes de publicar (no borrador) una clase `PROPIA`, el profesor debe tener su perfil profesional completo: al menos especialidad/disciplina principal y años de experiencia cargados.

- **Implementación:** `create-class/index.ts:81-93`. Si falta, responde `409` con `code: "PROFILE_INCOMPLETE"`. No aplica a clases `ASIGNADA` ni a borradores (`draft: true`).
- **Frontend:** el dashboard del profesor muestra una alerta inmediata cuando el perfil está incompleto (antes de que intente publicar y reciba el error).

---

## R04.2 — MercadoPago conectado obligatorio (para publicar clase `PROPIA`)

Antes de publicar (no borrador) una clase `PROPIA`, el profesor debe tener su cuenta de MercadoPago conectada (`mp_seller_accounts.status = 'CONNECTED'`) — es la cuenta a la que se le liquidará el pago cuando la clase se confirme como realizada (`teacher_payouts`).

- **Implementación (aplicación):** `create-class/index.ts:95-103`. Si no está conectada, responde `409` con `code: "MP_NOT_CONNECTED"`.
- **Implementación (autoridad real, a nivel de BD):** trigger `enforce_teacher_mp_connected` (`supabase/migrations/20260622000400_enforce_teacher_mp_connected.sql`), que bloquea directamente cualquier `INSERT`/`UPDATE` que deje una clase en `status = 'PUBLISHED'` si el profesor no tiene `mp_seller_accounts.status = 'CONNECTED'` — cierra la misma regla aunque alguien se salte la Edge Function.
- **Conexión:** OAuth de MercadoPago vía `mp-oauth-start` / `mp-oauth-callback` (distinto del OAuth de la sede, `mp-connect-start`/`mp-connect-callback`, usado para el split de arriendo de salas).

---

## R05 — Unicidad de documento de identidad (aviso, no bloqueo duro)

Al ingresar un RUT en la verificación de identidad, el sistema avisa si ese RUT ya está en uso por otra cuenta — en estado `PENDING` **o** `APROBADO`/`APPROVED` (no solo `APROBADO`), o registrado en `profiles.rut` de otra cuenta. El aviso ("RUT existente en la plataforma. Revisa que tus datos sean correctos") no impide técnicamente el envío en el backend, pero el frontend bloquea el submit si el RPC devuelve `true`.

- **Implementación:** RPC `rut_ya_registrado(p_rut)` (`supabase/migrations/20260711000000_rut_exists_rpc.sql`), `SECURITY DEFINER`, normaliza el RUT y excluye la cuenta propia (`auth.uid()`). Se invoca desde `IdentityUploadPage.vue` → `userService.rutYaRegistrado()`.

---

## R06 — Aprobación de sedes

Las sedes registradas por un Administrador de Sede deben ser aprobadas por un Admin General antes de ser visibles públicamente y poder recibir reservas. Al aprobar, además se le asigna automáticamente el rol `VENUE_ADMIN` al dueño de la sede y se marca `profiles.tiene_sede_aprobada = true` (ver R08).

- **Estados:** `PENDIENTE_APROBACION` → `APROBADA` o `RECHAZADA` (también existe `SUSPENDIDA`, enum `estado_sede`).
- **Implementación:** Edge Function `admin-approve-venue/index.ts:11-77` (rol `ADMIN`); registro de la sede en `register-venue/index.ts` (exige identidad validada, reutiliza una solicitud `RECHAZADA` previa del mismo admin si existe en vez de crear una nueva).

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

## R09 — Reembolso siempre vía MercadoPago (no hay "método preferido")

> ⚠️ **Regla desactualizada — corregida.** La tabla `refund_methods` existe en el esquema (bancario/billetera) pero **no se usa en ningún reembolso real**. Todo reembolso se procesa **siempre** por la API de MercadoPago, devolviendo al mismo medio de pago con el que se cobró originalmente — nunca consulta ni usa un "método preferido" configurado por el alumno.

- **Implementación:** `process-refunds/index.ts` (`processPaymentRefund`/`resolveMercadoPagoPaymentId`), cron cada 10 min. Reembolso **parcial** por el monto exacto de la inscripción, con `X-Idempotency-Key` por `payment.id` (ver R13).

---

## R10 — Unicidad de inscripción (por beneficiario, no por alumno)

Un mismo **beneficiario** no puede inscribirse dos veces en la misma clase, ni siquiera mediante carritos separados. Un mismo alumno **sí puede** inscribir a varios beneficiarios distintos (a sí mismo + asociados) en la misma clase.

> ⚠️ **Constraint desactualizado — corregido.** El `UNIQUE (class_id, beneficiary_type, beneficiary_id)` original bloqueaba que un alumno inscribiera más de un beneficiario en la misma clase (`beneficiary_id` es `NULL` para `SELF`, y Postgres trata cada `NULL` como distinto salvo con `COALESCE`). Reemplazado el 06-jul-2026.

- **Implementación:** índice `enrollments_unique_beneficiary` sobre `(class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))` (`supabase/migrations/20260706000000_fix_enrollments_unique.sql`).

---

## R11 — Procesamiento del checkout (no es atómico "todo o nada")

> ⚠️ **Regla desactualizada — corregida.** El checkout **no** es atómico en el sentido de "todos o ninguno". `mercadopago-webhook` procesa cada ítem del carrito en un **loop independiente**: si un ítem falla (p. ej. la clase ya no está en un estado inscribible o se llenó el cupo), se registra en `audit_logs` (`enrollment.skipped`) y se **continúa** con el resto del carrito — el pago ya fue cobrado por MercadoPago como una sola transacción, así que un ítem fallido no revierte el cobro ni las inscripciones ya creadas de otros ítems.
>
> La garantía real es más acotada: por cada ítem, la creación de `enrollments` + `payments` (RETAINED) es la única vía de escritura (vía `service_role`), y el trigger `enforce_class_capacity` (R02) evita sobrecupo con `FOR UPDATE`. No hay reversión de ítems ya procesados si otro falla.

- **Implementación:** `mercadopago-webhook/index.ts` (loop de ítems del carrito, `INSCRIBIBLE = ["PUBLISHED","POR_VALIDAR","FULL"]`).

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
Hoy la función **siempre falla** salvo que exista la variable de entorno
`MP_PAYOUT_MODE=live`, y aun en ese modo `disburseToSeller` no tiene
implementación real — el cron no llega a marcar ningún payout `PAID`.

**Honorario fijo en clases `ASIGNADA`:** el cálculo del `net_amount` del payout
difiere según `tipo_clase`. Para `PROPIA`, `net_amount = gross - comisión` de la
plataforma. Para `ASIGNADA`, el profesor dependiente recibe el `honorario` fijo
pactado por la sede al crear la clase (`commission_amount: 0`) —
`confirm-class/index.ts:60-89`.

**Fee real de MercadoPago y ciclo de corte:** el webhook guarda el costo real
que cobra MercadoPago por cada pago (`payment_sessions.mp_fee_amount` /
`net_received_amount`, solo en pagos nuevos — los históricos quedan `NULL`).
El panel de admin (`admin-payments`, `/admin/pagos`) agrupa los giros pendientes
por un **ciclo de corte mensual** configurable (`app_settings.payout_cutoff_day`,
default día 24) y muestra el margen real (comisión cobrada − costo MP) para
ajustar el % de comisión.

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

## R15 — Sugerencia de fechas según agenda real ⚠️ no implementada así (mecanismo huérfano)

> ⚠️ **No se corresponde con el código actual.** El mecanismo al que apunta esta regla (`propose-reschedule`) no calcula ni sugiere fechas: quien propone (ADMIN/VENUE_ADMIN) indica `proposedTime` manualmente en el body, sin consultar `room_schedule_blocks` ni `room_maintenances` — no valida siquiera que el horario propuesto esté libre. Y como se explica en la nota de cabecera de este documento, `propose-reschedule` ya no tiene ningún punto de entrada desde el frontend (huérfano desde el 19-jul-2026).
>
> El mecanismo que **sí** sugiere horarios reales de la agenda de sala hoy es R16.1/R16.2 (`teacher-reschedule-class`, `sede-reschedule-class`): ambos leen `room_schedule_blocks` con `status = 'AVAILABLE'` y solo permiten elegir entre esos bloques.

- **Implementación (huérfana):** `propose-reschedule/index.ts:17-42` — recibe `proposedTime` sin validar disponibilidad.

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

## R17 — Reagendamiento masivo si Maestro rechaza ⚠️ corregida (mecanismo huérfano)

> ⚠️ **La afirmación "la clase queda en `CANCELLED`" es falsa en el código actual.** `teacher-decision/index.ts:62-78` (rama `accepted: false`): la propuesta pasa a `reschedules.status = 'TEACHER_REJECTED'` y **todos** los pagos `RETAINED` de las inscripciones activas pasan a `REFUND_PENDING` con notificación — eso sí ocurre. Pero **la clase nunca se actualiza**: su `status` y su `start_time`/`end_time` originales quedan intactos (nunca se movieron, porque el `UPDATE classes` solo corre en la rama `accepted: true`). No existe ningún `UPDATE classes SET status = 'CANCELLED'` en todo el código — el valor `CANCELLED` del enum `class_status` es hoy **inalcanzable**.

- **Implementación (huérfana):** `teacher-decision/index.ts:62-78`.

---

## R18 — Quién decide el reagendamiento según tipo de clase ⚠️ no implementada así (mecanismo huérfano)

> ⚠️ **No se corresponde con el código actual.** `teacher-decision/index.ts:24-28` no consulta `tipo_clase` en absoluto: permite decidir a **cualquiera** de: el profesor dueño de la clase, un `VENUE_ADMIN` (cualquiera, no necesariamente el de esa sede), o un `ADMIN`. No hay distinción `PROPIA`/`ASIGNADA`. Además, quien *propone* (`propose-reschedule`) es el ADMIN/VENUE_ADMIN, no el Maestro — es decir, el flujo real es la inversa de lo que describe esta tabla (la sede propone, el profesor decide), no "cada tipo decide su propio dueño".
>
> La distinción `PROPIA` (decide/paga el profesor) vs. `ASIGNADA` (decide/gestiona la sede, sin pago) **sí** está correctamente implementada, pero en el mecanismo vigente R16.1/R16.2, no en este.

- **Implementación (huérfana):** `teacher-decision/index.ts:24-28`.

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

### Reagendamiento (mecanismo vigente: R16 / R16.1 / R16.2)

```
[clase caída (R16.1) o profe mueve su clase publicada (R16.2)]
                                ↓
                    reschedules TEACHER_ACCEPTED (48h)
                                ↓
                  ┌─ alumno acepta → REAGENDADO
                  ├─ alumno rechaza → payments REFUND_PENDING (solo ese alumno)
                  └─ timeout 48h → payments REFUND_PENDING (solo ese alumno)
```

> El diagrama "PROPUESTO → DECISION_MAESTRO → ... → CANCELLED" de versiones
> anteriores de este documento correspondía al mecanismo huérfano de R15/R17/R18
> (`propose-reschedule` + `teacher-decision`), que **nunca** deja la clase en
> `CANCELLED` en el código real (ver R17) y no tiene punto de entrada en la UI.

---

## Trazabilidad de implementación

Rutas reales al 19-jul-2026 (Deno/TypeScript en `supabase/functions/`, SQL en `supabase/migrations/`, Vue en `frontend/src/`). El backend Spring Boot de versiones anteriores de esta tabla **ya no existe** en el repositorio.

| Regla | Archivo de implementación | Verificación |
|---|---|---|
| R01 | `supabase/functions/confirm-class/index.ts` | Manual + SQL Editor (`payments.status`, `teacher_payouts`) |
| R02 | `supabase/functions/create-class/index.ts:142-150`, `mercadopago-webhook/index.ts` + trigger `enforce_class_capacity` (`20260707120000_enforce_class_capacity.sql`) + índice `enrollments_unique_beneficiary` | Manual + prueba de concurrencia |
| R03 | Trigger `trg_classes_status` → `track_class_status()` (`20260619000200_tables.sql`) | Manual (`class_status_history`) |
| R04 | `supabase/functions/create-class/index.ts:71-77`, `frontend/src/views/IdentityUploadPage.vue` | Manual |
| R04.1 | `supabase/functions/create-class/index.ts:81-93` | Manual |
| R04.2 | `supabase/functions/create-class/index.ts:95-103` + trigger `enforce_teacher_mp_connected` (`20260622000400_enforce_teacher_mp_connected.sql`) | Manual |
| R05 | RPC `rut_ya_registrado()` (`20260711000000_rut_exists_rpc.sql`), `frontend/src/services/userService.js` | Manual |
| R06 | `supabase/functions/admin-approve-venue/index.ts`, `register-venue/index.ts` | Manual |
| R07 | Tabla `associates`, `cart_items.beneficiary_type` | Integration |
| R08 | Asignación de rol en `create-class/index.ts:184-195`, `assign-reserva/index.ts`, `admin-approve-venue/index.ts:34-39` | Integration |
| R09 | `supabase/functions/process-refunds/index.ts` | Manual |
| R10 | Índice `enrollments_unique_beneficiary` (`20260706000000_fix_enrollments_unique.sql`) | BD |
| R11 | `supabase/functions/mercadopago-webhook/index.ts` (loop de ítems, no transaccional) | Manual + `frontend/src/views/CartPage.test.js` |
| R12 | `mercadopago-create-preference/index.ts` + `mercadopago-webhook/index.ts` | Sandbox MercadoPago |
| R13 | `confirm-class/index.ts`, `process-refunds/index.ts`, `process-payouts/index.ts`, `20260708120000_class_reschedule_window.sql` | Manual + SQL Editor |
| R14 | `frontend/src/components/ConfirmModal.vue` | `frontend/src/views/CartPage.test.js` |
| R15 | `propose-reschedule/index.ts` (huérfana, sin UI) | — |
| R16 | `student-decision/index.ts` + `frontend/src/views/alumno/AlumnoReagendamientoPage.vue` + cron `process_reschedule_timeouts` | Manual |
| R16.1 | `sede-reschedule-class/index.ts`, `20260708120000_class_reschedule_window.sql`, cron `process_class_reschedule_timeouts` | Manual end-to-end |
| R16.2 | `teacher-reschedule-class/index.ts`, `frontend/src/views/profesor/ProfesorReagendamientoPage.vue` | Manual end-to-end |
| R17 | `teacher-decision/index.ts:62-78` (huérfana, sin UI) | — |
| R18 | `teacher-decision/index.ts:24-28` (huérfana, sin UI) | — |
