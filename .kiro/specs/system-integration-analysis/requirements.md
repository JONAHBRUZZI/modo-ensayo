# Requirements Document

## Introduction

Este spec define un trabajo de **análisis de integración del sistema**, no la construcción de una feature. El sistema "Modo Ensayo" fue migrado de Spring Boot (Java 21) a Supabase (PostgreSQL 16, PostgREST, Auth, Storage, Realtime y Edge Functions en Deno) con un frontend Vue 3. El objetivo es producir un conjunto de **entregables de análisis** que mapeen la integración real entre backend y frontend, analicen a fondo el flujo de pagos retenidos de MercadoPago, e identifiquen brechas, inconsistencias y riesgos, incluyendo el *drift* entre el código del repositorio y la base de datos hosteada en producción.

El alcance abarca: 27 tablas con RLS, enums, funciones RPC (`get_my_attributes`, `handle_new_user`), triggers, jobs de `pg_cron`, 15 Edge Functions en `supabase/functions/`, la capa de `services/*` del frontend, el store de autenticación sobre Supabase Auth, y las vistas por rol (alumno, profesor, sede, admin). Se dispone del power `supabase-hosted` para consultar la base hosteada en vivo y verificar el drift.

El resultado del análisis es documentación (mapeos, diagramas de flujo, matriz de riesgos/brechas e informe de drift), sin modificación de código de producción.

## Glossary

- **Analysis_System**: El proceso de análisis definido por este spec y el conjunto de entregables documentales que produce.
- **Repo_Backend**: El backend de Supabase tal como está declarado en el repositorio: migraciones SQL en `supabase/migrations/`, Edge Functions en `supabase/functions/` y `supabase/config.toml`.
- **Hosted_Backend**: La instancia de Supabase en producción (base de datos, políticas RLS reales, jobs de `pg_cron` activos, enums y funciones desplegadas), consultable mediante el power `supabase-hosted`.
- **Frontend_Layer**: El frontend Vue 3, incluyendo `frontend/src/services/*`, `frontend/src/stores/auth.js` y las vistas por rol.
- **Payment_Flow**: El ciclo de pagos retenidos de MercadoPago: `mercadopago-create-preference` → checkout → `mercadopago-webhook` (crea `payments` en `RETAINED`) → `confirm-class` (`RETAINED`→`RELEASED`) → `teacher-decision`/`student-decision` (`RETAINED`→`REFUND_PENDING`).
- **Payment_Status**: El enum `payment_status` con valores `RETAINED`, `RELEASED`, `REFUND_PENDING`, `REFUNDED`, `FAILED`.
- **Drift**: Toda diferencia verificable entre Repo_Backend y Hosted_Backend (políticas RLS, jobs de `pg_cron`, enums, funciones, triggers o esquema de tablas).
- **Gap_Risk_Matrix**: Entregable tabular que registra cada brecha o riesgo detectado, su categoría, severidad, evidencia y recomendación.
- **Backend_Map**: Entregable que documenta el inventario y las relaciones del Repo_Backend (tablas, RLS, enums, RPC, triggers, `pg_cron`, Edge Functions).
- **Frontend_Backend_Map**: Entregable que documenta la correspondencia entre Frontend_Layer y las superficies de acceso a datos (PostgREST, Auth, Edge Functions, RPC).
- **Severity_Level**: Clasificación de severidad de una brecha o riesgo con valores `CRITICO`, `ALTO`, `MEDIO`, `BAJO`.

## Requirements

### Requirement 1: Mapeo del backend Supabase

**User Story:** Como arquitecto del proyecto, quiero un mapeo completo del backend Supabase declarado en el repositorio, para entender la superficie real del sistema migrado.

#### Acceptance Criteria

1. THE Analysis_System SHALL producir un Backend_Map que inventaríe las 27 tablas del esquema `public` con su propósito y relaciones de clave foránea.
2. THE Analysis_System SHALL documentar en el Backend_Map cada enum definido en `supabase/migrations/` con su nombre y valores.
3. THE Analysis_System SHALL documentar en el Backend_Map las funciones RPC `get_my_attributes` y `handle_new_user`, indicando entradas, salidas y efectos.
4. THE Analysis_System SHALL documentar en el Backend_Map los triggers y los jobs de `pg_cron` declarados, indicando la tabla o evento que los dispara y su efecto.
5. THE Analysis_System SHALL inventariar en el Backend_Map las 15 Edge Functions de `supabase/functions/`, indicando para cada una su propósito y su configuración de `verify_jwt` según `supabase/config.toml`.

### Requirement 2: Mapeo de la integración frontend ↔ backend

**User Story:** Como desarrollador, quiero un mapeo de cómo el frontend Vue 3 consume el backend, para localizar acoplamientos y puntos de fallo.

#### Acceptance Criteria

1. THE Analysis_System SHALL producir un Frontend_Backend_Map que asocie cada módulo de `frontend/src/services/*` con las tablas PostgREST, Edge Functions y RPC que invoca.
2. THE Analysis_System SHALL documentar en el Frontend_Backend_Map la relación entre `frontend/src/stores/auth.js` y Supabase Auth, incluyendo el manejo de sesión, refresh de token y los roles del claim `app_metadata.roles`.
3. THE Analysis_System SHALL documentar en el Frontend_Backend_Map las vistas por rol (alumno, profesor, sede, admin) y las superficies de datos que cada rol consume.
4. WHERE el frontend normaliza respuestas de snake_case a camelCase con el helper `camelize`, THE Analysis_System SHALL registrar este punto de adaptación como dependencia de contrato entre Frontend_Layer y backend.

### Requirement 3: Análisis del flujo de pagos retenidos de MercadoPago

**User Story:** Como responsable del producto, quiero un análisis a fondo del flujo de pagos retenidos, para asegurar que el dinero de los alumnos se libera o devuelve correctamente.

#### Acceptance Criteria

1. THE Analysis_System SHALL producir un diagrama de flujo del Payment_Flow que represente las transiciones del Payment_Status desde la creación de preferencia hasta `REFUNDED` o `FAILED`.
2. THE Analysis_System SHALL documentar para cada transición del Payment_Status el componente que la ejecuta entre `mercadopago-create-preference`, `mercadopago-webhook`, `confirm-class`, `teacher-decision` y `student-decision`.
3. THE Analysis_System SHALL analizar la idempotencia del `mercadopago-webhook` ante notificaciones duplicadas y registrar el resultado en la Gap_Risk_Matrix.
4. WHEN una clase asociada a pagos retenidos se cancela, THE Analysis_System SHALL documentar el efecto resultante sobre el Payment_Status de esos pagos.
5. WHEN un reagendamiento afecta a una clase con pagos retenidos, THE Analysis_System SHALL documentar el efecto resultante sobre el Payment_Status de esos pagos.
6. IF el análisis detecta una transición de Payment_Status no manejada, una condición de carrera o una vía que deje un pago en estado inconsistente, THEN THE Analysis_System SHALL registrar cada hallazgo en la Gap_Risk_Matrix con su evidencia.

### Requirement 4: Detección de brechas, inconsistencias y riesgos

**User Story:** Como líder técnico, quiero una matriz consolidada de brechas y riesgos, para priorizar las correcciones del sistema.

#### Acceptance Criteria

1. THE Analysis_System SHALL producir una Gap_Risk_Matrix que registre cada brecha o riesgo con categoría, descripción, evidencia, Severity_Level y recomendación.
2. THE Analysis_System SHALL evaluar las políticas RLS de cada tabla del esquema `public` y registrar en la Gap_Risk_Matrix toda tabla sin RLS efectivo o con políticas que permitan acceso no autorizado.
3. THE Analysis_System SHALL analizar los puntos frágiles del agendamiento, incluyendo la generación de bloques (`generate-blocks`, `room_schedule_blocks`) y los timeouts de reagendamiento, y registrar los hallazgos en la Gap_Risk_Matrix.
4. THE Analysis_System SHALL asignar a cada entrada de la Gap_Risk_Matrix un Severity_Level dentro de `CRITICO`, `ALTO`, `MEDIO` o `BAJO`.
5. WHERE una Edge Function expone una operación sensible sin `verify_jwt` distinta de los webhooks de pago, THE Analysis_System SHALL registrar el caso en la Gap_Risk_Matrix.

### Requirement 5: Verificación de drift entre repositorio y producción

**User Story:** Como operador del sistema, quiero verificar el drift entre el código del repositorio y la base hosteada, para confiar en que lo desplegado coincide con lo versionado.

#### Acceptance Criteria

1. THE Analysis_System SHALL consultar el Hosted_Backend mediante el power `supabase-hosted` para obtener las políticas RLS reales, los jobs de `pg_cron` activos, los enums y las funciones desplegadas.
2. THE Analysis_System SHALL comparar el Repo_Backend con el Hosted_Backend y producir un informe de Drift que liste cada diferencia detectada.
3. IF una política RLS, un enum, un trigger, un job de `pg_cron` o una función existe en el Hosted_Backend pero no en el Repo_Backend o difiere de su declaración, THEN THE Analysis_System SHALL registrar el Drift en el informe con la representación de ambos lados.
4. IF el power `supabase-hosted` no está disponible o una consulta al Hosted_Backend falla, THEN THE Analysis_System SHALL registrar el alcance verificado y la verificación pendiente en el informe de Drift.
5. THE Analysis_System SHALL registrar como Drift toda discrepancia entre la documentación del proyecto y el esquema real, incluyendo el modelo de pagos heredado de Spring Boot (`consolidated_payments`, `payment_items`) frente a la tabla `payments` actual.
