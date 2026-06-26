# Implementation Plan: Análisis de Integración del Sistema

## Overview

El "trabajo" de este spec es analítico: cada tarea ejecuta una capa del análisis definido en el diseño y redacta su contenido como un **capítulo interno** de un **único documento consolidado** en español: `Documentación/10-Analisis-Integracion.md`. No se crean archivos separados ni una carpeta `analysis/`.

Las capas son secuenciales (Capa 1 → 2 → 3 → 4 → 5) con dependencias explícitas. La primera tarea crea el documento con el esqueleto de capítulos (Resumen Ejecutivo, 1. Mapa del Backend Supabase, 2. Mapeo Frontend↔Backend, 3. Flujo de Pagos MercadoPago, 4. Matriz de Brechas y Riesgos, 5. Informe de Drift) e inicializa la tabla del capítulo 4. Cada capa posterior **escribe o actualiza su capítulo** dentro de ese mismo documento, y la **Matriz de Brechas y Riesgos** (capítulo 4) se alimenta incrementalmente desde varias capas.

Como **todas las tareas escriben sobre el mismo archivo** (`Documentación/10-Analisis-Integracion.md`), el grafo de dependencias las coloca en **oleadas estrictamente secuenciales** para evitar conflictos de escritura. El análisis es de solo lectura sobre el repositorio y consulta el `Hosted_Backend` únicamente con lecturas a través del power `supabase-hosted`.

## Tasks

- [x] 1. Crear el documento consolidado e inicializar la Matriz de Brechas y Riesgos
  - [x] 1.1 Crear `Documentación/10-Analisis-Integracion.md` con el esqueleto de capítulos
    - Crear el archivo `Documentación/10-Analisis-Integracion.md` con título y los capítulos vacíos en orden: **Resumen Ejecutivo**, **1. Mapa del Backend Supabase**, **2. Mapeo Frontend ↔ Backend**, **3. Flujo de Pagos MercadoPago**, **4. Matriz de Brechas y Riesgos**, **5. Informe de Drift repo ↔ producción**
    - En el capítulo **4. Matriz de Brechas y Riesgos**, inicializar la cabecera de tabla (`ID | Categoría | Descripción | Evidencia (archivo:línea / consulta) | Severity_Level | Recomendación`), la leyenda de categorías {RLS, Pagos, Agendamiento, Auth, Edge Function, Drift, Contrato Frontend, Otro} y los criterios de severidad {CRITICO, ALTO, MEDIO, BAJO}
    - Dejar la matriz lista para que cada capa la alimente incrementalmente
    - _Requirements: 4.1, 4.4_

- [x] 2. Capa 1 — Capítulo 1: Mapa del Backend Supabase
  - [x] 2.1 Inventariar tablas, relaciones de clave foránea y enums
    - Parsear `supabase/migrations/20260619000200_tables.sql` y migraciones de alteración (p. ej. `20260620000000_classes_discipline_nullable.sql`) para extraer las 27 tablas del esquema `public`, columnas, PKs y FKs; construir el grafo de FKs
    - Parsear `supabase/migrations/20260619000100_enums.sql` para extraer cada `CREATE TYPE ... AS ENUM` con su nombre y valores (incluido `payment_status`)
    - En el capítulo **1. Mapa del Backend Supabase** escribir las subsecciones **Tablas** (con columna "RLS habilitada"), **Diagrama de relaciones** (Mermaid `erDiagram`) y **Enums**
    - Documentar el conteo real observado y cualquier desviación frente a las 27 tablas esperadas
    - _Requirements: 1.1, 1.2_

  - [x] 2.2 Documentar funciones RPC, triggers y jobs de pg_cron
    - Parsear `20260619000101_helpers.sql`, `20260620010000_get_my_attributes.sql` y `20260620010100_handle_new_user_extra_fields.sql` para documentar `get_my_attributes` y `handle_new_user` (firma, entradas, salidas, efectos, `SECURITY DEFINER`/`INVOKER`)
    - Parsear `20260619000500_cron_functions.sql` y las definiciones de triggers para listar cada trigger (tabla, evento, función, efecto) y cada job de `pg_cron` (agenda, comando, efecto)
    - Añadir al capítulo **1. Mapa del Backend Supabase** las subsecciones **Funciones RPC**, **Triggers** y **Jobs pg_cron**
    - _Requirements: 1.3, 1.4_

  - [x] 2.3 Documentar RLS declarada e inventariar las Edge Functions
    - Parsear `20260619000250_helpers_rls.sql` y `20260619000300_rls_policies.sql` para registrar por tabla si tiene `ENABLE ROW LEVEL SECURITY` y el texto de cada política (`USING`/`WITH CHECK`)
    - Enumerar los 15 directorios de `supabase/functions/` (excluyendo `_shared/`); para cada uno registrar propósito (a partir de `index.ts`) y resolver `verify_jwt` desde `supabase/config.toml`
    - Añadir al capítulo **1. Mapa del Backend Supabase** la subsección **Edge Functions** y completar la columna "RLS habilitada" de la subsección Tablas; documentar el conteo real frente a las 15 Edge Functions esperadas
    - _Requirements: 1.5, 4.2_

  - [x] 2.4 Registrar brechas iniciales en la Matriz de Brechas y Riesgos
    - Registrar toda tabla del esquema `public` sin RLS declarado efectivo, con evidencia (archivo:línea)
    - Registrar toda Edge Function con `verify_jwt = false` que no sea un webhook de pago, con evidencia (`config.toml`)
    - Añadir las entradas al capítulo **4. Matriz de Brechas y Riesgos** con categoría y Severity_Level
    - _Requirements: 4.2, 4.5_

  - [x]* 2.5 Verificar completitud del Backend_Map
    - **Property 1: Completitud del Backend_Map**
    - Contrastar el conjunto de objetos del Repo_Backend (tablas, enums, triggers, jobs `pg_cron`, Edge Functions salvo `_shared/`) contra las entradas del capítulo 1; confirmar que cada enum incluye sus valores y cada Edge Function su `verify_jwt`
    - **Feature: system-integration-analysis, Property 1**
    - **Validates: Requirements 1.1, 1.2, 1.4, 1.5**

- [x] 3. Checkpoint — Capa 1 completa
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Capa 2 — Capítulo 2: Mapeo Frontend ↔ Backend
  - [x] 4.1 Mapear servicios del frontend a sus superficies de datos
    - Para cada archivo de `frontend/src/services/*.js`, localizar `supabase.from('<tabla>')` (PostgREST), `supabase.functions.invoke('<fn>')` (Edge Functions) y `supabase.rpc('<fn>')` (RPC)
    - En el capítulo **2. Mapeo Frontend ↔ Backend** escribir la subsección **Servicios → backend** (`Servicio | Tablas | Edge Functions | RPC`), una fila por cada servicio (conjunto posiblemente vacío)
    - _Requirements: 2.1_

  - [x] 4.2 Documentar autenticación, vistas por rol y contrato camelize
    - Analizar `frontend/src/stores/auth.js`: inicialización de sesión, `onAuthStateChange`, refresh de token y derivación de roles desde `app_metadata.roles`; documentar su propagación a la UI y a los guards de `frontend/src/router/index.js`
    - Mapear las vistas de cada rol (alumno, profesor, sede, admin) y las superficies de datos que consumen
    - Localizar el helper `camelize` y registrar cada punto de normalización snake_case ↔ camelCase como dependencia de contrato
    - Añadir al capítulo **2. Mapeo Frontend ↔ Backend** las subsecciones **Autenticación**, **Vistas por rol** y **Dependencias de contrato**
    - _Requirements: 2.2, 2.3, 2.4_

  - [x]* 4.3 Verificar completitud del mapeo de servicios
    - **Property 2: Completitud del mapeo de servicios**
    - Confirmar que cada archivo de `frontend/src/services/*.js` tiene exactamente una fila en la subsección **Servicios → backend** del capítulo 2
    - **Feature: system-integration-analysis, Property 2**
    - **Validates: Requirements 2.1**

- [x] 5. Capa 3 — Capítulo 3: Flujo de Pagos MercadoPago
  - [x] 5.1 Extraer transiciones y construir el diagrama de estados
    - Analizar `mercadopago-create-preference`, `mercadopago-webhook`, `confirm-class`, `teacher-decision` y `student-decision` para identificar cada escritura a `payments.status` con su valor origen/destino y condición
    - En el capítulo **3. Flujo de Pagos MercadoPago** escribir el diagrama Mermaid `stateDiagram-v2` de `payment_status` y la **Tabla de transiciones** (`Origen | Destino | Componente | Condición | Idempotente`), anotando el componente dueño de cada arista
    - _Requirements: 3.1, 3.2_

  - [x] 5.2 Documentar escenarios de negocio: cancelación, reagendamiento y reembolso
    - Documentar el efecto sobre `payment_status` cuando una clase con pagos retenidos se cancela, cuando un reagendamiento afecta a una clase con pagos retenidos, y el camino completo `RETAINED → REFUND_PENDING → REFUNDED`
    - Añadir al capítulo **3. Flujo de Pagos MercadoPago** las subsecciones **Escenarios** indicando el efecto y quién dispara el paso final del reembolso
    - _Requirements: 3.4, 3.5_

  - [x] 5.3 Analizar idempotencia del webhook y registrar el resultado
    - Revisar `mercadopago-webhook` para determinar si una notificación duplicada (mismo `data.id`) produce un segundo INSERT o transición; buscar índice único, `upsert (on conflict)` o verificación previa
    - Registrar la conclusión en el capítulo **4. Matriz de Brechas y Riesgos** (mínimo `ALTO`; `CRITICO` si puede provocar doble movimiento de dinero) con evidencia archivo:línea
    - _Requirements: 3.3_

  - [x] 5.4 Registrar inconsistencias del Payment_Flow en la Matriz de Brechas y Riesgos
    - Identificar transiciones de `payment_status` no manejadas, condiciones de carrera (p. ej. webhook concurrente con `confirm-class`) y vías que dejen un pago en estado inconsistente
    - Añadir cada hallazgo al capítulo **4. Matriz de Brechas y Riesgos** con evidencia (archivo:línea/fragmento) y Severity_Level
    - _Requirements: 3.6_

  - [x]* 5.5 Verificar cobertura de estados y dueño de cada transición
    - **Property 3: Cobertura de estados del Payment_Flow**
    - **Property 4: Componente dueño de cada transición**
    - Confirmar que los 5 valores de `payment_status` aparecen en el diagrama del capítulo 3 y que cada transición pertenece al conjunto de las 5 Edge Functions de pago
    - **Feature: system-integration-analysis, Property 3 y Property 4**
    - **Validates: Requirements 3.1, 3.2**

- [x] 6. Capa 3 (complemento) — Análisis del agendamiento (capítulo 4)
  - [x] 6.1 Analizar puntos frágiles del agendamiento y registrar hallazgos
    - Analizar la generación de bloques (`generate-blocks`, `room_schedule_blocks`) y los timeouts de reagendamiento (jobs de `pg_cron`, `propose-reschedule`) y su interacción con pagos `RETAINED`
    - Añadir los hallazgos al capítulo **4. Matriz de Brechas y Riesgos** con evidencia y Severity_Level
    - _Requirements: 4.3_

- [x] 7. Checkpoint — Capas 2 y 3 completas
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Capa 4 — Capítulo 5: Informe de Drift repo ↔ producción
  - [x] 8.1 Consultar el Hosted_Backend con el power supabase-hosted (solo lectura)
    - Leer el steering `supabase-hosted-database-workflow.md` y resolver el `project_id` con `list_projects`/`get_project`
    - Ejecutar solo lecturas: `list_tables` (schemas public, verbose), `get_advisors` (security y performance), `execute_sql` sobre `pg_policies`, `cron.job`, enums (`pg_type`/`pg_enum`), funciones (`pg_proc`), triggers (`information_schema.triggers`), `list_edge_functions` y `list_extensions`
    - En el capítulo **5. Informe de Drift repo ↔ producción** escribir la subsección **Alcance verificado** (herramienta + consulta + `project_id`); si el power no está disponible o una consulta falla, registrar la subsección **Verificación pendiente** y los pasos exactos para reintentar
    - _Requirements: 5.1, 5.4_

  - [x] 8.2 Contrastar Repo vs Hosted y producir el informe de Drift
    - Para cada categoría (tablas, columnas, RLS, enums, funciones, triggers, `pg_cron`, Edge Functions), comparar el conjunto del Repo_Backend (Capa 1, capítulo 1) con el del Hosted_Backend y clasificar cada diferencia como `SOLO_REPO`, `SOLO_HOSTED` o `DIFIERE`
    - Verificar el modelo de pagos heredado de Spring Boot (`consolidated_payments`/`payment_items`) frente a la tabla `payments` actual y registrar cualquier discrepancia con la documentación
    - Añadir al capítulo **5. Informe de Drift repo ↔ producción** las subsecciones **Diferencias** (con representación de ambos lados) y **Modelo de pagos heredado**
    - _Requirements: 5.2, 5.3, 5.5_

  - [x] 8.3 Registrar brechas de seguridad/RLS del Hosted en la Matriz de Brechas y Riesgos
    - Cruzar `get_advisors(security)` con la lectura de `pg_policies` para registrar toda tabla sin RLS efectivo o con políticas que permitan acceso no autorizado, evitando falsos positivos por `SECURITY DEFINER`
    - Añadir las entradas al capítulo **4. Matriz de Brechas y Riesgos** con evidencia (consulta/herramienta) y Severity_Level
    - _Requirements: 4.2_

  - [x]* 8.4 Verificar completitud y buena formación del diff de Drift
    - **Property 7: Completitud y buena formación del diff de Drift**
    - Confirmar que cada objeto del Repo o del Hosted está clasificado como coincidente o como diferencia, y que `DIFIERE` incluye ambos lados no nulos mientras `SOLO_REPO`/`SOLO_HOSTED` incluye el lado correspondiente
    - **Feature: system-integration-analysis, Property 7**
    - **Validates: Requirements 5.2, 5.3**

- [x] 9. Capa 5 — Consolidación y Resumen Ejecutivo
  - [x] 9.1 Normalizar Severity_Level y depurar la Matriz de Brechas y Riesgos
    - Revisar el capítulo **4. Matriz de Brechas y Riesgos** completo, asignar/normalizar el `Severity_Level` de cada entrada dentro de {CRITICO, ALTO, MEDIO, BAJO} y ordenar por severidad
    - Asegurar que categoría, descripción, evidencia y recomendación de cada entrada son no vacíos
    - _Requirements: 4.1, 4.4_

  - [x] 9.2 Redactar el Resumen Ejecutivo del documento consolidado
    - Completar la sección **Resumen Ejecutivo** de `Documentación/10-Analisis-Integracion.md`, sintetizando los hallazgos ordenados por severidad y enlazando a los capítulos de detalle (1–5)
    - Dar foco prioritario a MercadoPago (idempotencia del webhook, cancelación, reagendamiento, reembolso, transiciones inconsistentes)
    - _Requirements: 3.1, 4.1, 5.2_

  - [x]* 9.3 Verificar buena formación y exhaustividad de la Matriz de Brechas y Riesgos
    - **Property 5: Buena formación de la Gap_Risk_Matrix**
    - **Property 6: Exhaustividad del registro de riesgos**
    - Confirmar campos no vacíos y dominio de Severity_Level en el capítulo 4, y que toda tabla sin RLS efectivo, toda Edge Function `verify_jwt = false` no-webhook y todo hallazgo del Payment_Flow tienen entrada con evidencia
    - **Feature: system-integration-analysis, Property 5 y Property 6**
    - **Validates: Requirements 4.1, 4.4, 3.6, 4.2, 4.5**

- [x] 10. Checkpoint final — Documento consolidado completo
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Las tareas marcadas con `*` son chequeos de completitud/consistencia (tipo propiedad) opcionales y pueden omitirse para un MVP del análisis; el resto son entregables centrales.
- Todo el análisis se redacta como capítulos internos de un único documento: `Documentación/10-Analisis-Integracion.md`. No se crean archivos separados ni carpeta `analysis/`.
- Cada tarea referencia cláusulas específicas de los requisitos para trazabilidad.
- El capítulo 4 (Matriz de Brechas y Riesgos) se alimenta incrementalmente desde las Capas 1, 3, 3-complemento y 4. Como **todas las tareas escriben sobre el mismo archivo**, el grafo de dependencias usa **oleadas estrictamente secuenciales** (una tarea por oleada) para evitar conflictos de escritura.
- Los chequeos de propiedad referencian su propiedad de diseño con la etiqueta **Feature: system-integration-analysis, Property N**.
- El power `supabase-hosted` se usa exclusivamente con operaciones de lectura sobre catálogos del sistema; no se leen datos de usuarios ni se modifica el esquema.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2"] },
    { "id": 3, "tasks": ["2.3"] },
    { "id": 4, "tasks": ["2.4"] },
    { "id": 5, "tasks": ["2.5"] },
    { "id": 6, "tasks": ["4.1"] },
    { "id": 7, "tasks": ["4.2"] },
    { "id": 8, "tasks": ["4.3"] },
    { "id": 9, "tasks": ["5.1"] },
    { "id": 10, "tasks": ["5.2"] },
    { "id": 11, "tasks": ["5.3"] },
    { "id": 12, "tasks": ["5.4"] },
    { "id": 13, "tasks": ["5.5"] },
    { "id": 14, "tasks": ["6.1"] },
    { "id": 15, "tasks": ["8.1"] },
    { "id": 16, "tasks": ["8.2"] },
    { "id": 17, "tasks": ["8.3"] },
    { "id": 18, "tasks": ["8.4"] },
    { "id": 19, "tasks": ["9.1"] },
    { "id": 20, "tasks": ["9.2"] },
    { "id": 21, "tasks": ["9.3"] }
  ]
}
```
