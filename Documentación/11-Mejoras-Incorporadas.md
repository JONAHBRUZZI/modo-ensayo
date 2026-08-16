# Mejoras Incorporadas · Modo Ensayo

> **Versión:** 1.0 — 07-jul-2026
> Registro detallado de las mejoras y correcciones incorporadas que **no** estaban
> reflejadas en la documentación base (02-Reglas, 05-Modelo, 06-Endpoints). Sirve
> como changelog funcional. Cada entrada indica **qué**, **por qué**, **archivos** y
> **estado de despliegue**.

## Convenciones de despliegue

- **Frontend** (`frontend/`): se publica en Vercel **automáticamente al mergear a `main`**.
- **Edge Functions** (`supabase/functions/`): requieren `supabase functions deploy <fn>` (CLI).
- **Migraciones SQL** (`supabase/migrations/`): se aplican en el **SQL Editor** de Supabase.

---

## 1. Descubrimiento de clases (alumno)

### 1.1 Datos fundamentales visibles en las vistas de clase — PR #31
**Qué:** las vistas de clase del alumno muestran ahora **rango de edad** (`min_age`/`max_age`),
**dirección**, **sede** y **profesor**:
- *Clases Disponibles* (`ClassesPage.vue`): profesor, sede + dirección y edad en cada card.
- *Detalle de clase* (`ClaseDetallePage.vue`): edad recomendada, profesor y bloque "Dónde se imparte" (sede, sala, dirección + comuna).
- *Mis Clases* (`MisClasesPage.vue`): profesor y sede/dirección.

**Por qué:** eran datos fundamentales para decidir una inscripción y no se mostraban.

**Cómo:** edad/dirección/sede ya venían en el join `room → venue`. El **nombre del profesor**
vive en `profiles` (RLS `profiles_select_own`), así que se resuelve con un RPC nuevo
`get_teacher_names(uuid[])` (`SECURITY DEFINER` acotado; ver 06-Endpoints).

**Archivos:** `views/ClassesPage.vue`, `views/ClaseDetallePage.vue`, `views/alumno/MisClasesPage.vue`,
`services/classService.js` (`attachTeacherNames`), `services/paymentService.js`,
`migrations/20260706120000_get_teacher_names.sql`.
**Estado:** frontend desplegado; migración **aplicada** en SQL Editor.

### 1.2 Selección de beneficiario al agregar al carrito — PR #31 / #32
**Qué:** al agregar una clase, el alumno elige **a quién inscribe** (titular por defecto +
lista de asociados vinculados). El **carrito** muestra "Para: {persona}" en cada ítem.
**Por qué:** un mismo carrito debe permitir inscribir a distintas personas (uno para sí, otro
para un hijo, etc.) y ver claramente a quién va cada clase antes de pagar.
**Cómo:** `ClaseDetallePage` carga `associateService.getAssociates()`; `CartPage` resuelve el
nombre del beneficiario (titular o asociado). El beneficiario ya fluía por
`beneficiary_type`/`beneficiary_id` hasta la inscripción; solo faltaba mostrarlo.
**Archivos:** `views/ClaseDetallePage.vue`, `views/CartPage.vue`.
**Estado:** frontend desplegado.

### 1.3 Fix: `getMyEnrollments` aplanado — PR #31
**Qué:** `getMyEnrollments` devolvía la clase anidada, pero *Mis Clases* esperaba campos planos
(`title`, `startTime`, `status`), por lo que quedaban en blanco y el estado mostrado era el del
enrollment, no el de la clase. Ahora aplana correctamente y expone sede/dirección/profesor +
nombre del beneficiario.
**Archivos:** `services/paymentService.js`.
**Estado:** frontend desplegado.

---

## 2. Inscripciones

### 2.1 Fix índice único de `enrollments` (multi-beneficiario) — PR #30
**Qué:** el constraint `UNIQUE (class_id, student_id)` impedía que un alumno inscribiera a **más
de un beneficiario** (o a sí mismo + un asociado) en la misma clase. Se reemplaza por el índice
`enrollments_unique_beneficiary` sobre `(class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))`.
**Archivos:** `migrations/20260706000000_fix_enrollments_unique.sql`.
**Estado:** migración **aplicada** en SQL Editor.

---

## 3. Reagendamiento del alumno — PR #30

**Qué:** UI completa para que el alumno responda un reagendamiento (el backend `student-decision`
ya existía; faltaba toda la vista):
- Página `AlumnoReagendamientoPage.vue` + ruta `/alumno/reagendamiento/:rescheduleId`: hora original vs propuesta, cuenta regresiva de 48 h, Aceptar/Rechazar (con aviso de reembolso al rechazar).
- Banner "Reagendamiento pendiente" en *Mis Clases*.
- Notificaciones de reagendamiento **accionables** (campana + página de notificaciones).
- `rescheduleService.getMyPendingReschedules()`.
**Archivos:** `views/alumno/AlumnoReagendamientoPage.vue`, `router/index.js`,
`views/alumno/MisClasesPage.vue`, `components/NotificationBell.vue`, `views/NotificacionesPage.vue`,
`utils/notificationRoute.js`, `services/rescheduleService.js`.
**Estado:** frontend desplegado (backend ya estaba).

---

## 4. Ciclo del dinero (pagos / reembolsos)

### 4.1 `confirm-class`: liberar bloques y cancelar inscripciones — PR #38 (desplegada)
**Qué:** al confirmar la clase, además de liberar/retener pagos:
- **COMPLETED**: libera los bloques de horario (`room_schedule_blocks` OCCUPIED → AVAILABLE).
- **SUSPENDED**: cancela las inscripciones (`enrollments` → CANCELLED) y libera los bloques.
**Por qué:** antes los bloques quedaban OCCUPIED para siempre y las inscripciones ACTIVE tras una suspensión.
**Estado:** Edge Function **desplegada**.

### 4.2 `process-refunds`: reembolso por MercadoPago, parcial y robusto — PRs #39 / #40 / #43 (desplegar)
**Qué (acumulado):**
- Siempre intenta **MercadoPago primero** (canal del pago original), sin ramificar por método bancario.
- Reembolso **parcial** por el **monto exacto de la inscripción** (`{ amount: payment.amount }`).
  Un pago de MP puede cubrir varias clases del carrito → se devuelve solo la clase afectada, no el total.
- **Idempotencia** con `X-Idempotency-Key: refund:{payment.id}` (evita doble reembolso con dinero real).
- Errores **permanentes** de MP (4xx) → `payments` a `FAILED` + `audit_logs` (`payment.refund_failed`)
  para atención manual, sin reintentar. **Transitorios** (5xx/429) se reintentan en la próxima pasada.
- Si no se resuelve el `mercado_pago_payment_id` → también `FAILED` (antes reintentaba indefinidamente).
**Analogía:** como devolver solo la carne de un carro de supermercado, no la compra completa.
**Estado:** Edge Function — **requiere `supabase functions deploy process-refunds`** (incluye el fix de PR #43).

### 4.3 `EstadoBadge`: estados de pago — PR #38
**Qué:** el badge mapea los estados de pago (`RETAINED`, `RELEASED`, `REFUND_PENDING`, `REFUNDED`,
`FAILED`, `PAID`) a etiquetas y colores; antes caían al fallback (texto crudo, azul).
**Archivos:** `components/EstadoBadge.vue`.
**Estado:** frontend desplegado.

### 4.4 Fix cupo en `mercadopago-webhook` (carrito) — PR #43 (desplegar)
**Qué:** el chequeo de cupo (`count >= capacity`) se hacía por ítem contra el `count` de la BD,
que no ve las inscripciones creadas en el mismo carrito hasta confirmarse cada insert. Un carrito
con **varias inscripciones a la misma clase** podía pasar el cupo. Se agrega un contador en memoria
(`agregadosPorClase`) por clase durante el proceso.
**Estado:** Edge Function — **requiere `supabase functions deploy mercadopago-webhook`**.

### 4.5 Cupo a prueba de concurrencia (control a nivel de BD) — PR #44 (migración)
**Qué:** trigger `BEFORE INSERT` `enforce_class_capacity` sobre `enrollments`: bloquea la fila de la
clase (`SELECT ... FOR UPDATE`) y cuenta las inscripciones ACTIVE antes de permitir el insert;
rechaza con `CLASS_FULL` si el cupo está lleno.
**Por qué:** cierra la carrera **entre carritos concurrentes** distintos (dos pagos casi simultáneos
a la misma clase) que el fix en memoria de 4.4 no cubría — el chequeo count→insert del webhook no
era atómico entre procesos.
**Cómo:** el lock de fila serializa las inscripciones concurrentes a la misma clase; usa `count(*)`
en vivo (sin columna denormalizada → sin drift) y `SECURITY DEFINER` para no quedar acotado por RLS.
El webhook no requiere cambios: ya maneja el insert fallido (si el trigger rechaza, no crea el pago).
**Archivos:** `migrations/20260707120000_enforce_class_capacity.sql`.
**Estado:** migración **aplicada** en SQL Editor (08-jul).

### 4.6 Panel de admin de pagos + costo real de MercadoPago + corte mensual — (desplegada)
**Qué:** panel de administración para gestionar el dinero que hoy solo se veía por SQL:
- **Recordatorio** de giros pendientes (clases validadas por la sede sin pagar al profesor),
  agrupados por profesor con total por profesor y por ciclo.
- **Día de corte mensual** configurable (`app_settings.payout_cutoff_day`, default 24): el panel
  agrupa los `teacher_payouts` por ese ciclo (ej. cerrar el 24, pagar antes del 26).
- **Costo real de MercadoPago** por transacción: el webhook guarda `mp_fee_amount` y
  `net_received_amount` en `payment_sessions`; el panel muestra **ingresos vs. comisión cobrada
  vs. costo MP = margen** del ciclo, para fijar un % de comisión con margen.
- **Giros manuales** (PENDING→PAID con referencia) y **reembolsos fallidos** (reintentar
  FAILED→REFUND_PENDING o marcar resuelto FAILED→REFUNDED).
**Por qué:** el desembolso real está bloqueado (Fase 0, money-out MercadoPago Chile), así que el
admin gira manualmente; necesitaba un recordatorio, agrupación por corte y ver el margen real.
**Cómo:** Edge Function `admin-payments` (rol ADMIN, service role) con acciones `list`, `finance`,
`markPayoutPaid`, `retryRefund`, `markRefundResolved`; el fee se extrae de `payment.fee_details`
en el webhook. No cambia el flujo de cobro ni a quién apunta el payout.
**Archivos:** `functions/admin-payments/index.ts`, `functions/mercadopago-webhook/index.ts`,
`migrations/20260707000000_mp_fee_and_cutoff.sql`, `views/admin/AdminPagosPage.vue`,
`views/admin/AdminDashboardPage.vue`, `services/adminService.js`, `router/index.js`, `config.toml`.
**Estado:** frontend desplegado; Edge Functions **desplegadas** y migración **aplicada** (07-jul).

---

## 5. Dashboard del profesor (UX)

| PR | Mejora |
|----|--------|
| #29 | Tarjetas de *Clases Propias* pulsables → abren el modal de detalle (`ClaseDetalleModal`). |
| #33 | Se muestra la sección "Clases Propias" (antes oculta por depender del flag de reservas). |
| #34 | Las 5 tarjetas de estadísticas son **pulsables** → llevan a su detalle (Clases Propias/Asignadas, Métricas, Pagos). |
| #35 | Se quitan los listados de clases del dashboard (redundantes con las estadísticas pulsables). |
| #36 | Se separa "Clases por Asignar" (salas reservadas) de "Clases Propias"; el título de la página de propias se alinea. |
| #37 | Se quitan de "Accesos rápidos" las tarjetas duplicadas (Clases Asignadas, Métricas, Pagos). |
| #41 | La estadística "Clases Propias" cuenta **solo clases publicadas** (excluye borradores) para coincidir con la página. |

**Estado:** frontend desplegado.

---

## 6. Documentación base — PR #42

Se actualizaron `02-Reglas-de-Negocio.md` (R13, R16), `05-Modelo-de-Datos.md` (funciones/crons)
y `06-API-Endpoints.md` (RPCs, `process-refunds`) al estado real del ciclo del dinero.

---

## 7. Asistencia — "pasar lista" (desplegada)

**Qué:** se rehízo el flujo de asistencia del profesor, que estaba roto:
- **Todos parten como Presente** por defecto; el profesor solo **desmarca ausentes** y guarda
  toda la lista de una (se asume que la mayoría asiste). Antes partían todos en ausente.
- La página carga los **inscritos reales** (`get_venue_class_students`) en vez de filas de
  asistencia vacías (antes mostraba "No hay alumnos" en una clase nueva).
- **Guardado corregido:** el marcado anterior enviaba `beneficiary_id` undefined → el INSERT
  violaba `NOT NULL` y **nunca guardaba**. Ahora una Edge Function `save-attendance` (service role)
  resuelve el beneficiario real y hace **upsert idempotente** (el profesor no tiene policy de
  UPDATE sobre `attendances`), así se puede corregir sin duplicar.
**Por qué:** sin esto la métrica M3 (asistencia) no tenía datos — el guardado no funcionaba.
**Archivos:** `views/profesor/AttendancePage.vue`, `functions/save-attendance/index.ts`,
`services/classService.js`, `migrations/20260708000000_attendance_upsert.sql` (índice único
`(class_id, beneficiary_id)` para el upsert), `config.toml`.
**Estado:** frontend desplegado; Edge Function **desplegada** y migración **aplicada** (08-jul).

---

## 8. Métricas de rendimiento reales + desglose por sede (desplegada)

**Qué:** las métricas del dashboard admin (M1–M5) mostraban valores fijos (M1/M2/M3 en 0%, M5 en
100%) porque **nadie las calculaba**. Ahora:
- Una Edge Function `admin-metrics` (ADMIN, service role) calcula datos reales, **globales y por sede**.
- Las **5 métricas son clicleables** → abren un modal con **explicación humanizada**, el valor
  global y una **tabla por sede** (M4 es global, no por sede).

Fórmulas:
| Métrica | Objetivo | Fórmula | Qué mide |
|---|---|---|---|
| M1 Ocupación | >80% | inscripciones ACTIVE ÷ capacidad de la sala (`rooms.capacity`) | Cuánto se aprovecha el aforo físico de las salas |
| M2 Conversión | >70% | sesiones APPROVED ÷ total sesiones | Cuántos checkouts terminan en pago (penaliza abandono) |
| M3 Asistencia | >90% | asistencias `present` ÷ marcas totales | Cuántos inscritos asistieron (sobre inscritos, no capacidad) |
| M4 Disponibilidad | >95% | latidos `uptime_checks` registrados ÷ esperados | % de tiempo en línea (latido interno cada 5 min); global |
| M5 Pagos exitosos | >98% | APPROVED ÷ (APPROVED + FAILED) | Salud técnica del cobro (excluye abandonos) |

Por sede, M2/M5 se atribuyen a la(s) sede(s) que toca cada sesión (arriendo por `venueId`;
inscripción por la sede de cada clase del carrito).
**Archivos:** `functions/admin-metrics/index.ts`, `views/admin/AdminDashboardPage.vue`,
`services/adminService.js`, `config.toml`.
**Estado:** frontend desplegado; Edge Function **desplegada** (08-jul). Depende de que los profes
pasen lista (sección 7) para que M3 tenga datos.

### 8.1 M4 Disponibilidad: latido interno (desplegada)
**Qué:** M4 dejó de ser un link externo (abría UptimeRobot). Ahora es una métrica real
**autocontenida**: `pg_cron` inserta un latido en `uptime_checks` cada 5 min y M4 = latidos
registrados ÷ esperados (ventana 7 días, desde el primer latido para no penalizar el arranque).
Todo dentro del dashboard, sin servicios externos.
**Archivos:** `migrations/20260708100000_uptime_heartbeat.sql`, `functions/admin-metrics/index.ts`,
`views/admin/AdminDashboardPage.vue`.
**Estado:** frontend desplegado; **requiere aplicar la migración + redeploy de `admin-metrics`**.

---

## 9. Comportamiento · Google Analytics 4 (desplegada)

**Qué:** métricas de uso reales en el dashboard admin, vía GA4 (gratis):
- **Tracking** (gtag) en el frontend, con `G-JLYZFQXYX8` por defecto (el Measurement ID es público);
  envía `page_view` por ruta del SPA.
- Sección **"Comportamiento"**: usuarios activos ahora, usuarios/sesiones/vistas de 7 días y top
  páginas; refresco cada 30s. Edge Function `ga-metrics` consulta la Data API de GA4 con una
  service account (JWT RS256), propiedad `544653068`.
**Por qué:** M4 mide disponibilidad (infra), no comportamiento; GA cubre el uso real de usuarios.
**Archivos:** `frontend/src/analytics.js`, `frontend/src/main.js`, `functions/ga-metrics/index.ts`,
`views/admin/AdminDashboardPage.vue`, `services/adminService.js`, `config.toml`.
**Secretos (no en el repo):** `GA_PROPERTY_ID`, `GA_SERVICE_ACCOUNT` (Supabase); `VITE_GA_MEASUREMENT_ID`
(opcional, Vercel — el código ya trae el default).
**Estado:** desplegado (08-jul).

---

## 10. Gestión de usuarios (admin): eliminación en cascada + mensajes de confirmación (desplegada)

**Qué:** en el panel de usuarios del admin, el botón **Eliminar** fallaba con "Error al eliminar
el usuario" cuando el usuario tenía datos asociados, porque muchas FKs a `auth.users` no tenían
acción de borrado (violación de llave foránea). Ahora:
- **Migración** `20260709000000_user_delete_cascade.sql`: recorre todas las FKs que apuntan a
  `auth.users` y las ajusta — NOT NULL (propiedad del usuario) → `ON DELETE CASCADE`; NULLABLE
  (revisor / actor de auditoría) → `ON DELETE SET NULL` (conserva el registro histórico). El
  borrado de un usuario ahora limpia sus datos en cascada.
- **`admin-users`**: la Edge Function ahora devuelve el **mensaje de error real** (antes un
  "Internal error" opaco), útil para el admin.
- **Frontend**: se agregan **mensajes de éxito** ("El usuario … ha sido eliminado", "… suspendido",
  "… activado"). Los modales de confirmación (con motivo obligatorio en suspender) ya existían.
**Archivos:** `migrations/20260709000000_user_delete_cascade.sql`, `functions/admin-users/index.ts`,
`views/admin/AdminUsuariosPage.vue`.
**Estado:** frontend desplegado; **requiere aplicar la migración + redeploy de `admin-users`**.

---

## 11. Reagendamiento de clase NO realizada (reembolso diferido 24h) — PR #47 (desplegada 10-jul)

**Qué:** al marcar una clase "no realizada", ya **no se reembolsa de inmediato**; se abre una
ventana de **24h** (`classes.reschedule_deadline`) para reagendar. Pagos quedan `RETAINED`,
inscripciones `ACTIVE`. Si nadie reagenda, el cron `process_class_reschedule_timeouts` (cada hora)
reembolsa. Al reagendar, los alumnos aceptan/rechazan la nueva fecha (reusa `student-decision`).

**Flujos:**
- **PROPIA** (profe independiente): `confirm-class` notifica al profe (`CLASS_RESCHEDULE_OFFERED`);
  en `/profesor/reagendamientos` (cuenta regresiva) pone motivo obligatorio y **paga** un arriendo
  nuevo (`reserve-room-preference` con la clase como `borradorId` + `rescheduleReason`); el webhook
  republica la clase y dispara la decisión del alumno.
- **ASIGNADA** (clase de sede): la sede reagenda en su sala **sin pago** (`sede-reschedule-class` →
  `/sede/reagendar/:classId`) y avisa al profesor dependiente.
- Ambos reusan `_shared/reschedule.ts` (crea `reschedules` TEACHER_ACCEPTED + `reschedule_responses`
  + notifica RESCHEDULE_PENDING, con el cron de 48h existente).

**Archivos:** migración `20260708120000_class_reschedule_window.sql`; EFs `confirm-class`,
`reserve-room-preference`, `mercadopago-webhook`, `sede-reschedule-class`, `_shared/reschedule.ts`;
frontend `ProfesorReagendamientosPage.vue`, `SedeReagendarClasePage.vue`, `SedeClasesPorConfirmarPage.vue`,
servicios y `notificationRoute.js`.
**Decisiones:** reloj único 24h; independiente paga arriendo, sede no. *Futuro:* reglas 7d/72h.
**Estado:** migración aplicada + EFs desplegadas + frontend en `main` (10-jul). **Prueba end-to-end pendiente.**

### 4.7 Fixes de robustez del webhook de MercadoPago — 11-jul-2026

**Qué (2 commits, mismo día):**
- **Inscribir aunque la clase haya cambiado de estado:** el webhook solo creaba la inscripción si
  la clase seguía `PUBLISHED`. Si entre el pago y la llegada del webhook la clase pasaba a
  `POR_VALIDAR` o `FULL` (por el cron de cierre de clase o por otro alumno llenando el cupo), el
  alumno quedaba **cobrado pero sin inscripción**, sin que nadie lo viera. Ahora acepta
  `PUBLISHED`/`POR_VALIDAR`/`FULL`, y cualquier ítem cobrado-pero-no-inscrito queda auditado en
  `audit_logs` (`enrollment.skipped`) para seguimiento/reembolso manual del admin.
- **HMAC como defensa en profundidad, no como bloqueo:** MercadoPago envía varias notificaciones
  por pago y no todas traen una firma que matchee `MERCADOPAGO_WEBHOOK_SECRET`; el webhook las
  rechazaba con 403 y perdía pagos legítimos (sesión atascada en `PENDING`, alumno cobrado y sin
  inscribir). Ahora el chequeo HMAC solo se **loguea** si falla; la verificación real es contra la
  **API de MercadoPago** (lee el pago con el token propio y cruza `external_reference` contra una
  `payment_session` creada por el propio backend).

**Por qué:** ambos son bugs de correctitud/seguridad en el punto más crítico del sistema de pagos —
alumnos cobrados sin quedar inscritos.
**Archivos:** `supabase/functions/mercadopago-webhook/index.ts`.
**Estado:** commits en `main` (11-jul). **Despliegue sin confirmar** — ver ítem 13 de la checklist.

## 12. Pendiente / fuera de alcance (para contexto)

- **Desembolso real a profesores**: `process-payouts` → `disburseToSeller` es un **stub de Fase 0**
  (money-out MercadoPago Chile sin definir). Los `teacher_payouts` quedan PENDING; el dinero no se gira.
- **Reembolso de arriendos de sala**: no hay flujo para anular/devolver un `ROOM_RESERVATION`.
- **Cupo entre carritos concurrentes**: ver 4.4.
- **Validaciones de perfil server-side**: hoy solo en frontend (faltan triggers/constraints).
- **Rendimiento**: paralelizar cascadas de llamadas en las vistas calientes.

---

## 13. Incidente: drift entre historial de migraciones local y remoto — 16-ago-2026

**Qué:** una auditoría del CLI (`supabase migration list`) mostró 22 migraciones locales
(`20260622000000` → `20260710000001`) como "no aplicadas" en remoto, y 4 timestamps en remoto sin
archivo local (`20260622192727`, `20260622192754`, `20260622192832`, `20260627060217`).

**Causa:** parte del trabajo de ese período se aplicó vía el conector MCP de Supabase
(`apply_migration`) en vez de `supabase db push` — el schema real quedó correcto, pero el CLI nunca
se enteró (su tabla de tracking `supabase_migrations.schema_migrations` no se actualizó con esos
nombres de archivo).

**Diagnóstico:** se comparó objeto por objeto (`supabase db dump --schema public`) contra las 22
migraciones "pendientes": **21 de 22 ya estaban aplicadas en el schema real** (solo era un problema
de tracking). La única realmente faltante era el trigger de seguridad
`enforce_teacher_mp_connected` (`20260622000400`) — bloquea publicar una clase sin MercadoPago
conectado. Se aplicó ese día.

**Resolución:**
1. `supabase migration repair --status reverted <4 timestamps huérfanos>`.
2. `supabase migration repair --status applied <21 migraciones ya presentes en el schema real>`.
3. `supabase db push --include-all` para aplicar la única migración realmente faltante.
4. Verificado con un nuevo `supabase db dump` que el trigger quedó creado.

**Lección para el equipo:** si se usa el conector MCP para aplicar cambios de schema, hay que
correr `supabase migration list` después para detectar drift, en vez de asumir que coincide con lo
que hizo `db push` en otra sesión. Ver también checklist de despliegue en `A1-Despliegue.md`.

---

## Acciones de despliegue pendientes de este documento

1. `supabase functions deploy process-refunds` (fix 4.2 / PR #43). ✅ desplegada 07-jul.
2. `supabase functions deploy mercadopago-webhook` (fix 4.4 + fee real 4.6). ✅ desplegada 07-jul.
3. Aplicar `20260707120000_enforce_class_capacity.sql` en el SQL Editor (fix 4.5 / PR #44). ✅ aplicada 08-jul.
4. `supabase functions deploy admin-payments` (panel de pagos 4.6). ✅ desplegada 07-jul.
5. Aplicar `20260707000000_mp_fee_and_cutoff.sql` en el SQL Editor (panel de pagos 4.6). ✅ aplicada 07-jul.
6. `supabase functions deploy save-attendance admin-metrics` (asistencia 7 + métricas 8). ✅ desplegadas 08-jul.
7. Aplicar `20260708000000_attendance_upsert.sql` en el SQL Editor (asistencia 7). ✅ aplicada 08-jul.
8. `supabase functions deploy ga-metrics` (Google Analytics 9) + secretos `GA_PROPERTY_ID`/`GA_SERVICE_ACCOUNT`. ✅ desplegada 08-jul.
9. Aplicar `20260708100000_uptime_heartbeat.sql` en el SQL Editor + `supabase functions deploy admin-metrics` (M4 latido 8.1). ✅ migración confirmada aplicada en remoto (auditoría 16-ago, tabla `uptime_checks` presente en el schema real); deploy de la función no verificado en esta sesión.
10. Aplicar `20260709000000_user_delete_cascade.sql` en el SQL Editor + `supabase functions deploy admin-users` (gestión de usuarios 10). ✅ migración confirmada aplicada en remoto (auditoría 16-ago, `ON DELETE CASCADE`/`SET NULL` verificados en los FKs a `auth.users`); deploy de la función no verificado en esta sesión.
11. Aplicar `20260708120000_class_reschedule_window.sql` + `supabase functions deploy confirm-class reserve-room-preference mercadopago-webhook sede-reschedule-class` (reagendamiento 11). ✅ aplicada + desplegadas 10-jul.
12. Aplicar `20260622000400_enforce_teacher_mp_connected.sql` (trigger que bloquea publicar una clase sin MercadoPago conectado). ⚠️ **Quedó sin aplicar desde el 22-jun** — detectado y corregido recién en la auditoría del 16-ago-2026 (ver sección 13). El resto de las 21 migraciones "pendientes" que reportaba el CLI ese día ya estaban aplicadas en el schema real; era solo un problema de tracking (ver 13).
13. `supabase functions deploy mercadopago-webhook` (fixes 4.7, 11-jul). ✅ desplegada 16-ago.
14. `supabase db push` de `20260710000002_full_delete_cascade.sql` (renombrada por colisión de timestamp con `venue_stats.sql`) y `20260711000000_rut_exists_rpc.sql` (aviso de RUT duplicado). ✅ aplicadas 16-ago.
