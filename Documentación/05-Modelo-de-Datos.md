# Modelo de Datos · Modo Ensayo

> **Motor:** PostgreSQL 16 (Supabase)
> **Esquema:** `public` (33 tablas) + `auth` (gestionado por Supabase Auth)
> **Seguridad:** Row Level Security (RLS) habilitado en todas las tablas
> **Generado a partir del schema real del proyecto hosteado.**

## 1. Identidad y autenticación

La identidad la gestiona **Supabase Auth** en el esquema `auth` (`auth.users`).
No existen tablas propias `users` / `roles` / `user_roles`: los roles viajan en
el claim `app_metadata.roles` del JWT. Cada usuario tiene un registro espejo en
`public.profiles` creado por el trigger `handle_new_user`.

### `profiles`
PK `id` = `auth.users.id`.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK, FK → auth.users(id) |
| full_name | text | NOT NULL |
| social_name | text | |
| phone | text | |
| rut | text | |
| identidad_validada | bool | DEFAULT false |
| identidad_estado | text | DEFAULT 'SIN_VALIDAR' |
| tiene_sede_aprobada | bool | DEFAULT false |
| preferred_refund_method_id | uuid | FK → refund_methods(id) |
| created_at / updated_at | timestamptz | |

### `professional_profiles`
Perfil de profesor. PK `id` = `auth.users.id`.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK / FK → auth.users(id) |
| description, biografia, especialidad, specialty | text | |
| disciplina_principal | text | |
| disciplinas_secundarias | text[] | DEFAULT '{}' |
| nivel_ensenanza, formacion, detalle_formacion | text | |
| tipo_formacion | text[] | DEFAULT '{}' |
| experience_years | int | |
| instagram, youtube, sitio_web, linkedin | text | |
| photo_url | text | |
| average_rating | numeric | |

### `identity_verifications`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → auth.users(id) |
| document_url, document_type, document_number | text | |
| full_name | text | |
| birth_date | date | |
| status | text | DEFAULT 'PENDING' |
| reviewed_by | uuid | |

### `associates`
Beneficiarios/familiares a cargo de un usuario.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| owner_id | uuid | FK → auth.users(id) |
| name | text | NOT NULL |
| email, relationship, rut | text | |
| birth_date | date | |
| status | text | DEFAULT 'ACTIVE' |

### `refund_methods`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → auth.users(id) |
| bank, account_type, account_number, account_holder, rut | text | datos bancarios |

## 2. Sedes y salas

### `venues`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| admin_id | uuid | FK → auth.users(id) |
| name | text | NOT NULL |
| city, region, comuna, address, description | text | |
| image_url, phone, email | text | |
| status | `estado_sede` | DEFAULT 'PENDIENTE_APROBACION' |
| tipo | `tipo_sede` | DEFAULT 'SEDE' |
| rejection_reason | text | |
| instagram, youtube, sitio_web, facebook | text | |

### `rooms`
Salas de una sede, con un amplio set de atributos de equipamiento.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| venue_id | uuid | FK → venues(id) |
| name | text | NOT NULL |
| capacity, tamano_m2 | int | |
| tipo_piso | `tipo_piso` | |
| floor_type, type, equipment, image_url | text | |
| price_per_hour | numeric | |
| activa | bool | DEFAULT true |
| has_mirrors, has_sound, tiene_barra_ballet, tiene_aire_acondicionado, tiene_calefaccion, tiene_insonorizacion, tiene_amplificacion, tiene_entrada_auxiliar, tiene_microfono, tiene_equipo_grabacion, tiene_piano, tiene_guitarra, tiene_bateria | bool | DEFAULT false |

### `venue_documents`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| venue_id | uuid | FK → venues(id) |
| file_url | text | NOT NULL |
| tipo | `tipo_documento_sede` | NOT NULL |
| nombre, tipo_archivo | text | |
| estado | text | DEFAULT 'PENDIENTE' |
| motivo_rechazo | text | |

### `venue_photos`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| owner_id | uuid | venue o room |
| owner_type | text | DEFAULT 'VENUE' ('VENUE'/'ROOM') |
| photo_url | text | NOT NULL |
| alt_text | text | |
| display_order | int | DEFAULT 0 |
| principal | bool | DEFAULT false |

### `venue_schedules`
Horario de apertura por día.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| venue_id | uuid | FK → venues(id) |
| day_of_week | text | |
| open_time, close_time | time | |

### `venue_block_configs`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| venue_id | uuid | FK → venues(id) |
| block_duration_min | int | DEFAULT 60 |
| gap_between_blocks_min | int | DEFAULT 15 |

### `room_schedule_blocks`
Bloques de agenda generados por sala.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| room_id | uuid | FK → rooms(id) |
| start_time, end_time | timestamptz | |
| status | `block_status` | DEFAULT 'AVAILABLE' |
| class_id | uuid | FK → classes(id) |
| held_until | timestamptz | Hasta cuándo está reservado temporalmente (HELD) |
| held_by | uuid | FK → auth.users(id); usuario que inició el pago |

### `room_maintenances`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| room_id | uuid | FK → rooms(id) |
| start_time, end_time | timestamptz | |
| reason | text | |
| created_by | uuid | |

## 3. Clases y asistencia

### `classes`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| title | text | NOT NULL |
| discipline, discipline_category | text | |
| level | `nivel_clase` | NOT NULL |
| description | text | |
| capacity, duration | int | NOT NULL |
| price | numeric | NOT NULL |
| min_age, max_age | int | |
| start_time, end_time | timestamptz | |
| room_id | uuid | FK → rooms(id) |
| teacher_id | uuid | FK → auth.users(id), NOT NULL |
| status | `class_status` | DEFAULT 'DRAFT' |
| tipo_clase | `tipo_clase` | DEFAULT 'PROPIA' |
| reschedule_deadline | timestamptz | Ventana de 24h para reagendar tras marcarse "no realizada"; NULL fuera de ella. El cron `process_class_reschedule_timeouts` reembolsa al vencer. |
| honorario | numeric | Solo ASIGNADA: monto fijo al profesor dependiente |

### `class_status_history`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| class_id | uuid | FK → classes(id) |
| previous_status, new_status | text | |
| changed_by | uuid | |

### `enrollments`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| class_id | uuid | FK → classes(id) |
| student_id | uuid | FK → auth.users(id) |
| beneficiary_type | text | DEFAULT 'SELF' |
| beneficiary_id | uuid | |
| status | text | DEFAULT 'ACTIVE' |

> **Unicidad:** índice único `enrollments_unique_beneficiary` sobre
> `(class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))`. Un mismo
> beneficiario no se repite en la clase, pero un alumno sí puede inscribir a
> varios beneficiarios distintos (él mismo + asociados) en la misma clase.

### `attendances`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| class_id | uuid | FK → classes(id) |
| beneficiary_id | uuid | |
| beneficiary_type | text | DEFAULT 'SELF' |
| present | bool | NOT NULL |
| marked_by | text | |

> Índice único `(class_id, beneficiary_id)` (migración `20260708000000`): una marca de
> asistencia por persona y clase, para el upsert idempotente de `save-attendance`.

## 4. Carrito y pagos

### `cart_items`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| owner_id | uuid | FK → auth.users(id) |
| class_id | uuid | FK → classes(id) |
| class_title, discipline, level | text | denormalizado |
| price | numeric | |
| beneficiary_type | text | DEFAULT 'SELF' |
| beneficiary_id | uuid | |

### `payment_sessions`
Sesión de checkout contra MercadoPago.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| owner_id | uuid | FK → auth.users(id) |
| external_reference | text | NOT NULL |
| preference_id | text | |
| cart_snapshot | jsonb | DEFAULT '{}' |
| status | `payment_session_status` | DEFAULT 'PENDING' |
| mercado_pago_payment_id | text | |
| processed_at | timestamptz | |
| mp_fee_amount | numeric | Comisión real que cobró MercadoPago en el pago (suma de `fee_details`). La llena el webhook; NULL en pagos históricos |
| net_received_amount | numeric | Neto efectivamente recibido (`transaction_details.net_received_amount`). La llena el webhook; NULL en históricos |

### `payments`
Pago retenido por inscripción (se libera cuando la clase se realiza).

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| enrollment_id | uuid | FK → enrollments(id) |
| amount | numeric | NOT NULL |
| status | `payment_status` | DEFAULT 'RETAINED' |

## 5. Reagendamiento

### `reschedules`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| class_id | uuid | FK → classes(id) |
| teacher_id | uuid | FK → auth.users(id) |
| proposed_time | timestamptz | NOT NULL |
| reason | text | |
| status | `reschedule_status` | DEFAULT 'PROPOSED' |
| response_deadline | timestamptz | |
| new_class_id | uuid | FK → classes(id) |

### `reschedule_responses`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| reschedule_id | uuid | FK → reschedules(id) |
| user_id | uuid | FK → auth.users(id) |
| response_type | `response_type` | |
| responded_at | timestamptz | |

## 6. Reseñas, notificaciones y operación

### `reviews`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| class_id | uuid | FK → classes(id) |
| reviewer_id | uuid | FK → auth.users(id) |
| target_type | `review_target_type` | NOT NULL |
| target_id | uuid | NOT NULL |
| score | int | NOT NULL (1–5) |
| comment | text | |

### `notifications`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → auth.users(id) |
| title, message | text | NOT NULL |
| type | text | |
| read | bool | DEFAULT false |

### `discipline_catalog`
Catálogo de disciplinas (semilla).

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| name, category | text | NOT NULL |
| active | bool | DEFAULT true |
| sort_order | int | DEFAULT 0 |

### `audit_logs`
| Columna | Tipo | Notas |
|---|---|---|
| id | bigint | PK |
| actor_id | uuid | |
| action, resource_type, resource_id | text | NOT NULL |
| old_values, new_values | jsonb | |
| metadata | jsonb | DEFAULT '{}' |
| ip | inet | |
| user_agent | text | |

### `system_metrics`
| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| metric_name | text | NOT NULL |
| metric_value | numeric | NOT NULL |
| labels | jsonb | DEFAULT '{}' |
| recorded_at | timestamptz | |

## 7. Marketplace MercadoPago Connect (arriendo de salas)

Cuando un profesor arrienda una sala, el pago se hace con **split automático**: la
sede recibe el monto directo en su cuenta de MercadoPago y la plataforma retiene
una comisión configurable. Cada sede vincula su cuenta vía OAuth.

### `mp_seller_accounts`
Cuenta de MercadoPago vinculada del gestor de sede (1 por usuario). Los tokens
**nunca** se exponen al frontend; solo las Edge Functions los leen.

| Columna | Tipo | Notas |
|---|---|---|
| user_id | uuid | PK, FK → auth.users(id) |
| mp_user_id | text | ID del vendedor en MercadoPago |
| access_token | text | Token del vendedor (solo service role) |
| refresh_token | text | Para renovar el access_token |
| token_expires_at | timestamptz | |
| scope | text | Scopes otorgados por MP |
| public_key | text | Clave pública del vendedor |
| status | text | CONNECTED / DISCONNECTED / ERROR |
| connected_at | timestamptz | DEFAULT now() |
| updated_at | timestamptz | |

Vista pública segura: `mp_seller_status` — expone solo `user_id`, `mp_user_id`,
`status`, `connected_at` y `has_token` (boolean). Sin tokens.

### `mp_oauth_states`
State temporal del flujo OAuth (anti-CSRF). Se borra al completar el callback.

| Columna | Tipo | Notas |
|---|---|---|
| state | text | PK |
| user_id | uuid | FK → auth.users(id) |
| created_at | timestamptz | DEFAULT now() |

### `app_settings` (claves relevantes)
Configuración de negocio editable sin redeploy (jsonb). Solo `service_role` la
escribe; el admin la lee/edita vía Edge Functions y policies `has_role('ADMIN')`.

| key | Tipo de valor | Descripción |
|-----|---------------|-------------|
| `room_reservation_commission_pct` | numeric (0–100) | Comisión de la plataforma sobre el arriendo. Default: 0 |
| `marketplace_commission_pct` | numeric (0–100) | Comisión de la plataforma sobre las clases. Default: 10 |
| `payout_cutoff_day` | numeric (1–28) | Día del mes en que cierra el ciclo de giros a profesores; el panel de admin agrupa los payouts por ese ciclo. Default: 24 |

### `teacher_payouts`
Registro del desembolso del honorario al profesor cuando su clase se confirma como
realizada (`COMPLETED`). Una fila por pago liberado; el giro real a MercadoPago queda
`tracked` (Fase 0). Origen: migración `20260622000100_marketplace_payouts.sql`.

| Columna | Tipo | Notas |
|---|---|---|
| id | uuid | PK |
| payment_id | uuid | UNIQUE, FK → payments(id) |
| teacher_id | uuid | FK → auth.users(id) |
| class_id | uuid | FK → classes(id) |
| gross_amount | numeric | Monto bruto del pago |
| commission_amount | numeric | DEFAULT 0 — comisión de la plataforma |
| net_amount | numeric | Neto a girar al profesor |
| mp_reference | text | Referencia del giro en MercadoPago |
| status | text | CHECK PENDING / PAID / FAILED |
| error_detail | text | Detalle si el giro falla |
| created_at | timestamptz | DEFAULT now() |
| paid_at | timestamptz | |

### `uptime_checks`
Latido de disponibilidad (M4). `pg_cron` inserta una fila cada 5 min; la métrica es
latidos registrados vs. esperados. Origen: `20260708100000_uptime_heartbeat.sql`.

| Columna | Tipo | Notas |
|---|---|---|
| id | bigint | PK (identity) |
| checked_at | timestamptz | DEFAULT now() |

## 8. Enums (tipos definidos)

| Enum | Valores |
|---|---|
| `block_status` | AVAILABLE, HELD, OCCUPIED, MAINTENANCE |
| `class_status` | DRAFT, PUBLISHED, IN_PROGRESS, FULL, CANCELLED, COMPLETED, SUSPENDED, POR_VALIDAR |
| `estado_sede` | PENDIENTE_APROBACION, APROBADA, RECHAZADA, SUSPENDIDA |
| `nivel_clase` | BASICO, INTERMEDIO, AVANZADO |
| `payment_session_status` | PENDING, APPROVED, FAILED |
| `payment_status` | RETAINED, RELEASED, REFUND_PENDING, REFUNDED, FAILED |
| `reschedule_status` | PROPOSED, TEACHER_ACCEPTED, TEACHER_REJECTED, COMPLETED |
| `response_type` | ACCEPTED, REJECTED, TIMEOUT, RECHAZADO_AUTOMATICO |
| `review_target_type` | CLASS, VENUE, STUDENT |
| `tipo_clase` | PROPIA, ASIGNADA |
| `tipo_documento_sede` | RUT_EMPRESA, CEDULA_IDENTIDAD, INICIO_ACTIVIDADES_F4415, CERTIFICADO_SITUACION_TRIBUTARIA, CONTRATO_ARRIENDO, COMPROBANTE_DOMICILIO, PERMISO_MUNICIPAL, CARPETA_TRIBUTARIA_ELECTRONICA, ESCRITURA_CONSTITUCION, AUTORIZACION_NOTARIAL_PROPIETARIO, CERTIFICADO_IVA, PATENTE_COMERCIAL, RESOLUCION_SANITARIA, OTRO |
| `tipo_piso` | MADERA, FLOTANTE, CERAMICO, VINILO, CEMENTO, ALFOMBRA, OTRO |
| `tipo_sede` | SEDE, HOME_STUDIO |

## 9. Seguridad y lógica en la base de datos

- **RLS**: todas las tablas de `public` tienen Row Level Security habilitado. El
  frontend usa la clave anon/publishable y solo accede a lo que las políticas
  permiten. Las operaciones privilegiadas pasan por Edge Functions con la clave
  de servicio.
- **Funciones / triggers**: `handle_new_user` (crea `profiles` al registrarse),
  `get_my_attributes` (atributos derivados del usuario), `get_teacher_names(uuid[])`
  (nombre público de profesores, `SECURITY DEFINER` acotado; salta la RLS de
  `profiles` para mostrar el profesor en las vistas de clases), `track_class_status`
  (audita transiciones en `class_status_history`), `enforce_class_capacity`
  (trigger `BEFORE INSERT` en `enrollments`: bloquea la fila de la clase con
  `FOR UPDATE` y rechaza si el cupo ACTIVE está lleno — cupo a prueba de
  concurrencia), `release_expired_holds`
  (libera bloques HELD vencidos cada 5 min vía `pg_cron`),
  `process_reschedule_timeouts` (timeout 48h de la decisión del alumno) y
  `process_class_reschedule_timeouts` (cada hora: reembolso diferido de las clases
  no realizadas que no se reagendaron dentro de las 24h), más helpers de RLS y
  jobs de `pg_cron` para tareas programadas (regeneración de bloques,
  `process-refunds` cada 10 min, `process-payouts` cada 15 min).
- **Migraciones**: el schema se versiona en `supabase/migrations/`. La base
  hosteada es la fuente de verdad; se sincroniza con la CLI.

> El schema histórico previo (tablas `users`/`roles`/`user_roles`,
> `consolidated_payments`, etc. del backend Spring Boot) quedó obsoleto con la
> migración a Supabase. Este documento refleja el schema actual real.
