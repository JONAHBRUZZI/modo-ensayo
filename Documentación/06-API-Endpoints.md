# API / Superficie de acceso a datos

Tras la migración a Supabase ya no existe un backend REST propio bajo `/api`.
El frontend accede a los datos por tres vías:

1. **Supabase Auth** — autenticación (login, registro, OAuth)
2. **PostgREST** — CRUD directo sobre las tablas, protegido por RLS
3. **Edge Functions** — lógica de negocio sensible y privilegiada

Todas las llamadas viajan con el JWT de Supabase; las políticas RLS deciden qué
puede ver o modificar cada usuario.

---

## 1. Autenticación (Supabase Auth)

No son endpoints REST propios: se invocan mediante el SDK
(`frontend/src/stores/auth.js`).

| Acción | SDK | Descripción |
|--------|-----|-------------|
| Registro | `supabase.auth.signUp` | Crea usuario; trigger `handle_new_user` crea su `profiles` |
| Login | `supabase.auth.signInWithPassword` | Email/password, retorna sesión JWT |
| Login Google | `supabase.auth.signInWithIdToken` | OAuth con ID token de Google |
| Logout | `supabase.auth.signOut` | Cierra sesión |
| Refresh | `supabase.auth.refreshSession` | Renueva el access token |

Los roles viajan en el claim `app_metadata.roles` del JWT.

---

## 2. Acceso a datos vía PostgREST

CRUD directo con `supabase.from('<tabla>')`, sujeto a las políticas RLS de cada
tabla. Ejemplos representativos (la autorización real la imponen las políticas):

| Tabla | Operaciones típicas | Quién (según RLS) |
|-------|---------------------|-------------------|
| `profiles` | `select` / `update` propio | Dueño del perfil |
| `classes` | `select` publicadas | Público / autenticado |
| `discipline_catalog` | `select` | Público |
| `venues` / `rooms` | `select` aprobadas | Público |
| `associates` | `select` / `insert` / `delete` | Dueño |
| `notifications` | `select` / `update` (leído) | Destinatario |
| `reviews` | `select` públicas | Público |
| `refund_methods` | CRUD propio | Dueño |

Funciones RPC (PostgreSQL) invocadas con `supabase.rpc(...)`:

| RPC | Descripción |
|-----|-------------|
| `get_my_attributes` | Atributos derivados del usuario (identidad, reservas, estado de profesor, sede, etc.) |
| `get_teacher_names(uuid[])` | Nombre público (`id` + `full_name`) de los profesores de una lista de clases. `SECURITY DEFINER` acotado: solo devuelve usuarios que son profesor de alguna clase, saltando la RLS de `profiles`. Lo usa el frontend para mostrar el profesor en las vistas de clases del alumno. |
| `get_venue_class_students(uuid)` | Alumnos inscritos + asistencia de una clase; autoriza al profesor de la clase o al admin de la sede. |
| `list_teacher_candidates()` | Candidatos (email + nombre) para asignar como profesor dependiente en una sede. |

---

## 3. Edge Functions (lógica de negocio)

Invocadas con `supabase.functions.invoke(name)` (helper `invokeFunction`).
Definidas en `supabase/functions/`. `verify_jwt` se configura por función en
`supabase/config.toml` (los webhooks van sin verificación de JWT).

| Función | Propósito |
|---------|-----------|
| `create-class` | Crea/publica una clase. Dos flujos con reglas de autorización propias: **`ASIGNADA`** (crea el `VENUE_ADMIN`/`ADMIN`, requiere `roomId`+`teacherId`, valida que la sala pertenezca a esa sede y que el profesor sea dependiente `ACTIVE` de esa misma sede vía `venue_teachers`) vs. **`PROPIA`** (crea el propio profesor; si publica, requiere identidad validada solo si ya tiene el rol `TEACHER` — R04 —, y además perfil profesional completo y MercadoPago conectado — R04.1/R04.2 —, con códigos `PROFILE_INCOMPLETE`/`MP_NOT_CONNECTED`). Asigna automáticamente el rol `TEACHER` si publica su primera clase sin tenerlo (R08). La capacidad se toma de `rooms.capacity`, no del formulario (R02) |
| `book-slot` | Marca un **bloque de horario de sala** (`room_schedule_blocks`) como `OCCUPIED` y lo vincula a una clase — no es una inscripción de alumno (eso ocurre en `mercadopago-webhook`). Usa `service_role` con guard atómico (`.eq('status','AVAILABLE')`) contra dobles reservas; autoriza al `TEACHER` dueño de la clase o al `VENUE_ADMIN` dueño de la sede |
| `assign-reserva` | Asigna un borrador (`DRAFT`) del profesor a una sala/horario concreto: valida que el borrador le pertenezca y esté en `DRAFT`, chequea conflicto de horario, reasigna los bloques `OCCUPIED` de la reserva-placeholder al borrador publicado, y asigna el rol `TEACHER` si el usuario no lo tiene aún |
| `confirm-class` | Solo el `VENUE_ADMIN` dueño de la sede o `ADMIN` confirma si una clase se realizó. Si `realized=true`: pagos `RETAINED→RELEASED`, crea `teacher_payouts` (con `honorario` fijo si es `ASIGNADA`, o `gross - comisión` si es `PROPIA`), libera bloques. Si `realized=false`: **no reembolsa de inmediato** — abre ventana de 24h de reagendamiento (R16.1), pagos quedan `RETAINED`, notifica al profesor solo si la clase es `PROPIA` |
| `propose-reschedule` ⚠️ | Solo `ADMIN` o el `VENUE_ADMIN` dueño de la sede puede proponer un reagendamiento (`reschedules` en `PROPOSED`) para una clase `PUBLISHED`, con una fecha arbitraria (no valida disponibilidad real de sala). **Huérfana**: ninguna vista del frontend la invoca desde el 19-jul-2026 (ver R15/R18) |
| `teacher-decision` ⚠️ | El profesor dueño de la clase, o cualquier `VENUE_ADMIN`/`ADMIN`, acepta o rechaza una propuesta `PROPOSED` — no distingue `tipo_clase` (ver R18). Al aceptar mueve `classes.start_time/end_time` **sin tocar `room_schedule_blocks`** y abre 48h para los alumnos. Al rechazar, pasa los pagos `RETAINED` de esa clase a `REFUND_PENDING` (la clase **no** queda `CANCELLED`, ver R17). **Huérfana**: sin caller en el frontend |
| `student-decision` | El alumno acepta o rechaza un reagendamiento propuesto (mecanismo R16, usado tanto por el flujo huérfano `teacher-decision` como por el vigente R16.1/R16.2 `triggerStudentReschedule`); al rechazar, sus `payments RETAINED` pasan a `REFUND_PENDING` |
| `generate-blocks` | El `VENUE_ADMIN`/`ADMIN` dispara manualmente `regenerate_schedule_blocks()` (el mismo RPC que corre semanalmente por cron) |
| `create-review` | Crea una reseña polimórfica (`CLASS`/`VENUE`/`STUDENT`, score 1–5); exige que el autor haya **participado** en la clase (inscrito `ACTIVE` o profesor de la clase) |
| `register-venue` | Registra una sede/home studio; exige identidad validada (403 si no) del usuario; si el mismo admin tiene una sede previa `RECHAZADA`, la **reutiliza** (actualiza) en vez de crear una nueva |
| `admin-approve-venue` | `ADMIN` aprueba o rechaza una sede. Al aprobar: `status='APROBADA'`, asigna el rol `VENUE_ADMIN` al dueño, marca `profiles.tiene_sede_aprobada=true` y notifica |
| `admin-stats` | Estadísticas de la plataforma (admin) |
| `admin-users` | Gestión de usuarios y roles (admin) |
| `admin-payments` | Panel de pagos (admin, service role). Acciones: `list` (giros `teacher_payouts` PENDING enriquecidos + reembolsos `payments` FAILED con su error de `audit_logs`), `finance` (costo real de MercadoPago vs. comisión cobrada = **margen** del ciclo de corte), `markPayoutPaid` (PENDING→PAID + `mp_reference`), `retryRefund` (FAILED→REFUND_PENDING) y `markRefundResolved` (FAILED→REFUNDED). No-admin → 403 |
| `admin-metrics` | Métricas de rendimiento del admin (service role): M1 Ocupación, M2 Conversión, M3 Asistencia, M5 Pagos exitosos **globales y por sede**, y M4 Disponibilidad **global** (uptime = latidos `uptime_checks` registrados vs. esperados). No-admin → 403 |
| `save-attendance` | Guarda la lista de asistencia de una clase de una vez (upsert idempotente). Autoriza al profesor de la clase o al admin de la sede; resuelve el beneficiario real de cada inscripción. Requiere service role (el profesor no tiene policy de UPDATE sobre `attendances`) |
| `ga-metrics` | Métricas de comportamiento desde **Google Analytics 4** (admin, service role): usuarios activos ahora, usuarios/sesiones/vistas de 7 días y top páginas. Autentica con service account (JWT RS256) a la Data API. Sin secretos `GA_PROPERTY_ID`/`GA_SERVICE_ACCOUNT` → `{ configured:false }` |
| `mercadopago-create-preference` | Crear preferencia de pago (inscripción a clases) |
| `mercadopago-webhook` | Webhook de notificaciones de pago (sin JWT); verifica firma HMAC, discrimina entre inscripción y reserva de sala, lee el pago con el token de la plataforma o de la sede vendedora, y guarda el **fee real de MercadoPago** (`mp_fee_amount`/`net_received_amount`) en `payment_sessions` |
| `mp-connect-start` | Inicia OAuth de MercadoPago Connect; genera state anti-CSRF y devuelve URL de autorización |
| `mp-connect-callback` | Callback OAuth (sin JWT); valida state, canjea code→tokens y guarda cuenta del vendedor |
| `reserve-room-preference` | Crea preferencia de arriendo de sala con split automático a la cuenta MercadoPago de la sede. Acepta `borradorId` (publica ese borrador/clase al pagar) y `rescheduleReason` (cuando el arriendo completa un reagendamiento de una clase caída) |
| `sede-reschedule-class` | La **sede** reagenda una clase ASIGNADA (no realizada) a una sala propia **sin pago**: ocupa bloques, republica la clase en el nuevo horario, dispara la decisión de los alumnos y avisa al profesor dependiente |
| `mp-oauth-start` | Inicia OAuth de MercadoPago para conectar la cuenta del **profesor** (payouts); genera `state` anti-CSRF en `mp_oauth_states` y devuelve la URL de autorización |
| `mp-oauth-callback` | Callback OAuth del profesor (sin JWT); valida `state`, canjea code→tokens y guarda la cuenta del vendedor |
| `process-refunds` | Batch (pg_cron `*/10`, service role): procesa `payments` en REFUND_PENDING vía API MercadoPago. Reembolso **parcial** por el monto exacto de la inscripción (`{ amount: payment.amount }`) con `X-Idempotency-Key` por `payment.id`, y pasa a REFUNDED de forma idempotente. Errores 4xx de MP (permanentes) → `FAILED` + `audit_logs` (`payment.refund_failed`) para atención manual; 5xx/429 se reintentan en la próxima pasada |
| `process-payouts` | Batch (pg_cron `*/15`, service role): intenta liquidar `teacher_payouts` en PENDING → PAID. **El desembolso real (`disburseToSeller`) es un stub de Fase 0** (money-out MercadoPago Chile pendiente): hoy la función **siempre falla** salvo que exista `MP_PAYOUT_MODE=live`, y aun en ese modo `disburseToSeller` no tiene implementación real — el cron no llega a marcar ningún payout `PAID` todavía |

> Nota: `mp-oauth-*` conecta la cuenta del **profesor** (para recibir el honorario
> vía `teacher_payouts`), mientras que `mp-connect-*` conecta la cuenta de la
> **sede** (para el split del arriendo de salas). Son dos flujos OAuth distintos.

---

## 4. Storage

Archivos (verificación de identidad, fotos de sedes, etc.) se suben con
`supabase.storage` a través de `frontend/src/services/uploadService.js`, que
mapea cada tipo lógico a su bucket y construye el path. El acceso a los buckets
también está regido por políticas.

---

> **Nota**: la tabla histórica de endpoints REST `/api/...` del backend Spring
> Boot quedó obsoleta con la migración. Este documento refleja la superficie
> actual basada en Supabase.
