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
| `create-class` | Crear/publicar una clase (validaciones de negocio) |
| `book-slot` | Reservar un cupo en una clase |
| `assign-reserva` | Asignar una reserva a una clase |
| `confirm-class` | Confirmar la realización de una clase |
| `propose-reschedule` | Proponer una reprogramación |
| `teacher-decision` | Decisión del profesor sobre reprogramación |
| `student-decision` | Decisión del alumno sobre reprogramación |
| `generate-blocks` | Generar bloques de horario |
| `create-review` | Crear una reseña |
| `register-venue` | Registrar una sede |
| `admin-approve-venue` | Aprobar/rechazar sede (admin) |
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
| `teacher-reschedule-class` | El **profesor** mueve una clase suya **publicada y aún no ocurrida** a otro horario de la **misma sala** que ya arrienda (**sin pago**): valida que sea el dueño de la clase, toma los bloques nuevos con guard atómico, libera los viejos, actualiza el horario y dispara la decisión de los alumnos |
| `mp-oauth-start` | Inicia OAuth de MercadoPago para conectar la cuenta del **profesor** (payouts); genera `state` anti-CSRF en `mp_oauth_states` y devuelve la URL de autorización |
| `mp-oauth-callback` | Callback OAuth del profesor (sin JWT); valida `state`, canjea code→tokens y guarda la cuenta del vendedor |
| `process-refunds` | Batch (pg_cron `*/10`, service role): procesa `payments` en REFUND_PENDING vía API MercadoPago. Reembolso **parcial** por el monto exacto de la inscripción (`{ amount: payment.amount }`) con `X-Idempotency-Key` por `payment.id`, y pasa a REFUNDED de forma idempotente. Errores 4xx de MP (permanentes) → `FAILED` + `audit_logs` (`payment.refund_failed`) para atención manual; 5xx/429 se reintentan en la próxima pasada |
| `process-payouts` | Batch (pg_cron `*/15`, service role): liquida `teacher_payouts` en PENDING → PAID. **El desembolso real (`disburseToSeller`) es un stub de Fase 0** (money-out MercadoPago Chile pendiente); crea el payout pero no gira dinero aún |

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
