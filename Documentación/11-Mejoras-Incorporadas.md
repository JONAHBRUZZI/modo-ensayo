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

## 10. Pendiente / fuera de alcance (para contexto)

- **Desembolso real a profesores**: `process-payouts` → `disburseToSeller` es un **stub de Fase 0**
  (money-out MercadoPago Chile sin definir). Los `teacher_payouts` quedan PENDING; el dinero no se gira.
- **Reembolso de arriendos de sala**: no hay flujo para anular/devolver un `ROOM_RESERVATION`.
- **Cupo entre carritos concurrentes**: ver 4.4.
- **Validaciones de perfil server-side**: hoy solo en frontend (faltan triggers/constraints).
- **Rendimiento**: paralelizar cascadas de llamadas en las vistas calientes.

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
9. Aplicar `20260708100000_uptime_heartbeat.sql` en el SQL Editor + `supabase functions deploy admin-metrics` (M4 latido 8.1). ⏳ pendiente.
