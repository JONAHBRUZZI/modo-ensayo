# Plan de Pruebas · Modo Ensayo

> **Versión:** 3.0 — 09-jul-2026 (previo a Evaluación Final)
> **Cobertura objetivo:** flujos críticos de negocio cubiertos por pruebas automatizadas +
> aceptación manual del 100% de las historias de usuario.

## 1. Objetivo

Asegurar que el MVP funcione correctamente bajo escenarios reales de uso, validando las 18
reglas de negocio, las 22 historias de usuario y los flujos críticos antes de la Evaluación
Final Transversal.

## 2. Estrategia general

Cuatro niveles de pruebas complementarias, adaptadas a una arquitectura Supabase (backend
como servicio) en vez de un servidor propio:

| Nivel | Herramienta | Objetivo | Cobertura |
|---|---|---|---|
| **Unitarias / componentes** | Vitest + `@vue/test-utils` (frontend) | Lógica de componentes Vue aislada | Vistas críticas de pago |
| **Basadas en propiedades** | Vitest + `fast-check` | Modelar reglas de negocio (reembolsos, estados de clase) contra un espacio grande de entradas, no solo casos puntuales | Reglas de reembolso y autorización |
| **Seguridad a nivel de datos** | Row Level Security (RLS) por tabla + Supabase Advisors | Verificar que ninguna tabla quede sin política de acceso | 100% de las tablas |
| **Aceptación manual** | Plan de escenarios + lista de verificación | Validar HU completas en producción | 100% HU |

## 3. Ambiente de pruebas

### Frontend
- Node.js 22 + Vitest
- `jsdom` para simular el DOM
- Mocks de los módulos de `services/` (Supabase) para aislar el componente bajo prueba

### Backend (Supabase)
- Las Edge Functions (Deno + TypeScript) se prueban de forma manual/funcional contra el
  proyecto Supabase (no hay mocks de infraestructura equivalentes a TestContainers; la
  superficie de prueba es la API real en modo sandbox).
- **RLS como capa de prueba de seguridad:** cada tabla tiene políticas declaradas en SQL; se
  verifican ejecutando `get_advisors` (seguridad + performance) tras cada cambio de schema,
  que detecta tablas sin RLS habilitado o políticas ausentes.

### Ambiente cloud para validación
- Frontend desplegado en **Vercel** (mismo pipeline que producción, con preview deployments
  por rama).
- Backend: proyecto **Supabase** de desarrollo, separado del de producción.
- MercadoPago en modo **sandbox**.

## 4. Tests automatizados implementados

### Frontend (7 archivos, 44 tests)

| Archivo | Tipo | Qué valida |
|---|---|---|
| `CartPage.test.js` | Componente | Renderizado del carrito, cálculo de total, confirmación antes de pagar |
| `PaymentSuccessPage.test.js` | Componente | Procesamiento de query params de retorno de MercadoPago |
| `__tests__/bugfix/e2e-refund-chain.test.ts` | Propiedades (fast-check) | Cadena completa de reembolso: clase suspendida → pago `RETAINED`→`REFUND_PENDING` → `process-refunds` → `REFUNDED` |
| `__tests__/bugfix/g06-refund-not-processed.property.test.ts` | Propiedades | Un pago en `REFUND_PENDING` siempre termina procesado (no queda huérfano) |
| `__tests__/bugfix/g07-suspended-class-orphan-payments.property.test.ts` | Propiedades | Al suspender una clase, ningún pago asociado queda en estado inconsistente |
| `__tests__/bugfix/g16-privileged-functions-no-auth.property.test.ts` | Propiedades | Las funciones privilegiadas (admin) rechazan cualquier request sin rol autorizado |
| `__tests__/bugfix/preservation.property.test.ts` | Propiedades | Invariantes de negocio se preservan ante secuencias arbitrarias de operaciones |

**Resultado de la última ejecución** (`npm run test`, 09-jul-2026): **42 de 44 tests pasan**
(6 de 7 archivos en verde). Los 2 tests que fallan son de temporización en el mock de
`CartPage.test.js` (el componente queda en estado "Cargando..." antes de que se resuelva el
mock) — no reflejan un bug del producto, sino un ajuste pendiente en el test. `npm run lint`
corre sin errores (0 errores, solo warnings de estilo) y `npm run build` genera el bundle de
producción sin fallos.

## 5. Plan de escenarios de aceptación

### 5.1 Escenarios del flujo crítico (P1)

**Escenario 1 — Registro y validación de identidad**
1. Usuario nuevo se registra (HU01)
2. Hace login (HU02)
3. Sube documento de identidad (HU03)
4. Admin General aprueba (HU21)
5. **Verificar:** `identidad_validada = true` en BD y banner desaparece

**Escenario 2 — Búsqueda, carrito y pago consolidado familiar**
1. Usuario crea 2 asociados (familiares)
2. Busca clase de "Danza Contemporánea" (HU05)
3. Agrega al carrito para sí mismo + asociado 1 + asociado 2 (HU07)
4. Hace checkout con confirmación (HU08)
5. Es redirigido a MercadoPago sandbox y completa pago
6. **Verificar:** 3 inscripciones creadas, 3 pagos en `RETAINED`

**Escenario 3 — Reserva de sala → rol Maestro → perfil profesional → publicar clase**
1. Alumno validado va a "Buscar Salas"
2. Reserva sala y paga el arriendo (split automático a la sede vía MercadoPago Connect)
3. **Verificar:** rol TEACHER asignado automáticamente
4. Completa biografía + disciplina (HU04)
5. Publica la clase asociada a la sala reservada
6. **Verificar:** clase en estado `PUBLISHED` visible en búsqueda

**Escenario 4 — Confirmación de clase realizada → liberación de pagos**
1. Maestro marca asistencia (todos presentes por defecto, ajusta ausentes)
2. Admin Sede confirma la clase como realizada
3. **Verificar:** pagos asociados pasan a `RELEASED`; giro queda como `PENDING` en el panel de
   pagos del admin (el desembolso final se marca manualmente hasta que MercadoPago Chile
   habilite el money-out automático)

**Escenario 5 — Confirmación de clase no realizada → devoluciones**
1. Admin Sede marca la clase como suspendida/no realizada
2. **Verificar:** pagos asociados pasan a `REFUND_PENDING` y luego a `REFUNDED` tras la
   siguiente pasada del job de reembolsos (`pg_cron`)

**Escenario 6 — Reagendamiento con timeout de 48h**
1. Maestro propone reagendamiento de una clase
2. Alumnos reciben notificación accionable
3. Alumno 1: acepta → su inscripción se mueve a la nueva fecha
4. Alumno 2: rechaza → su pago pasa a `REFUND_PENDING`
5. Alumno 3: no responde dentro de 48h → timeout automático (`pg_cron`) → `REFUND_PENDING`
6. **Verificar:** estado final correcto para los 3

**Escenario 7 — Métricas de rendimiento del admin**
1. Admin entra al dashboard, pincha cualquiera de las métricas M1-M5
2. **Verificar:** se abre el detalle con explicación y desglose por sede, con datos reales
   calculados por la Edge Function `admin-metrics`

**Escenario 8 — Cancelación de inscripción por Alumno**
1. Alumno ve "Mis Clases" y cancela una clase futura
2. Confirma con aviso de reembolso
3. **Verificar:** inscripción a `CANCELLED`, pago a `REFUND_PENDING`

### 5.2 Escenarios de borde y error (P2)

**Borde 1 — Identidad duplicada**
- Usuario intenta subir un documento de identidad ya validado por otra cuenta → rechazado.

**Borde 2 — Capacidad de clase llena**
- Trigger de base de datos (`enforce_class_capacity`) rechaza inscripciones cuando la clase
  ya alcanzó su capacidad, incluso ante dos pagos casi simultáneos (carrera resuelta con lock
  de fila).

**Borde 3 — Pagos concurrentes para el último cupo**
- Dos usuarios pagan simultáneamente por el último cupo de una clase.
- **Verificar:** solo uno logra inscribirse; el otro recibe el reembolso correspondiente.

**Borde 4 — Email duplicado en registro**
- Dos personas intentan registrarse con el mismo correo → Supabase Auth rechaza el segundo
  registro.

**Borde 5 — Maestro con perfil incompleto que intenta publicar**
- Puede publicar (no es bloqueante) pero ve un banner persistente de aviso hasta completar
  su perfil.

### 5.3 Escenarios de seguridad (P1)

**Seguridad 1 — Acceso sin JWT**
- Cualquier operación que requiere sesión, sin token válido → rechazada por RLS/Auth.

**Seguridad 2 — Acceso a datos ajenos**
- Un alumno intenta leer/cancelar la inscripción de otro alumno → bloqueado por política RLS
  (`enrollments` filtra por `student_id = auth.uid()`).

**Seguridad 3 — Acción de administrador ejecutada por un usuario normal**
- Un usuario sin rol `ADMIN` invoca una Edge Function administrativa (ej. `admin-payments`,
  `admin-users`) → responde `403 Forbidden`. Cubierto por el test de propiedades
  `g16-privileged-functions-no-auth.property.test.ts`.

**Seguridad 4 — Tablas sin política de RLS**
- `get_advisors` (seguridad) se corre tras cada migración de schema; cualquier tabla nueva
  sin política queda señalada antes de desplegar a producción.

### 5.4 Pruebas de disponibilidad y rendimiento

**Disponibilidad — Latido interno (M4)**
- `pg_cron` inserta un registro de "latido" cada 5 minutos en `uptime_checks`.
- **Verificar:** el porcentaje de disponibilidad (M4 del dashboard admin) se calcula como
  latidos registrados / latidos esperados en la ventana — si el sistema se cae, el hueco de
  latidos baja el porcentaje automáticamente, sin depender de un servicio externo.

**Rendimiento — Métricas de negocio en vivo**
- Las métricas M1 (ocupación de salas), M2 (conversión de pago), M3 (asistencia) y M5 (pagos
  exitosos) se calculan on-demand desde datos reales de producción, sirviendo también como
  monitoreo continuo de la salud del negocio.

## 5.5 Lista de verificación de aceptación — 22 HU

| HU | Descripción | Estado |
|---|---|---|
| HU01 | Registro con correo único | ✓ Verificado |
| HU02 | Login con JWT + manejo de credenciales inválidas | ✓ Verificado |
| HU03 | Upload de documento de identidad + validación | ✓ Verificado |
| HU04 | Perfil profesional del Maestro (biografía + disciplinas) | ✓ Verificado |
| HU05 | Búsqueda de clases con filtros | ✓ Verificado |
| HU06 | Ver detalle de clase con cupos disponibles | ✓ Verificado |
| HU07 | Carrito con beneficiarios, validar duplicados | ✓ Verificado |
| HU08 | Checkout consolidado MercadoPago + estado `RETAINED` | ✓ Verificado |
| HU09 | Cancelar inscripción → `REFUND_PENDING` | ✓ Verificado |
| HU10 | Reservar sala → asignar rol TEACHER | ✓ Verificado |
| HU11 | Crear borrador sin sala → asignar sala después | ✓ Verificado |
| HU12 | Publicar clase con validaciones | ✓ Verificado |
| HU13 | Marcar asistencia (todos presentes por defecto) | ✓ Verificado |
| HU14 | Dashboard Alumno: mis clases + estados | ✓ Verificado |
| HU15 | Registrar sede con documentos requeridos | ✓ Verificado |
| HU16 | Registrar sala con equipamiento | ✓ Verificado |
| HU17 | Admin Sede confirma clase realizada/no realizada | ✓ Verificado |
| HU18 | Reagendamiento con sugerencias + decisión del alumno en 48h | ✓ Verificado |
| HU19 | Panel Admin General: usuarios, sedes, pagos, métricas por sede | ✓ Verificado |
| HU20 | Dejar reseña post-clase completada | ✓ Verificado |
| HU21 | Admin General aprueba identidad / sede | ✓ Verificado |
| HU22 | Cambio de contexto (Alumno/Profesor/Sede/Admin) sin recargar sesión | ✓ Verificado |

**22 / 22 historias de usuario verificadas.**

## 6. Métricas de aceptación

| Métrica | Objetivo | Estado actual |
|---|---|---|
| Tests automatizados en verde | 100% | 42/44 (95%) — 2 fallas de temporización en mocks, sin impacto en el producto |
| `npm run lint` sin errores | Sí | ✓ 0 errores |
| `npm run build` exitoso | Sí | ✓ genera el bundle de producción sin fallos |
| Bugs críticos al cierre | 0 | 0 bugs críticos conocidos |
| HU verificadas manualmente | 22 / 22 | ✓ 22 / 22 |
| Tablas sin política RLS | 0 | 0 (verificado con `get_advisors`) |
| Disponibilidad medida (M4) | > 95% | Latido interno activo (`uptime_checks`) |

## 7. Responsabilidades

| Área | Responsable |
|---|---|
| Tests de componentes y propiedades (frontend) | Victor |
| Edge Functions y reglas de negocio en BD | Jonathan |
| RLS, migraciones y `get_advisors` | Darlette |
| Aceptación manual de HU | Equipo completo |
| Revisión de seguridad (RLS + Edge Functions privilegiadas) | Equipo completo |

## 8. Herramientas

- **Frontend:** Vitest, `@vue/test-utils`, `jsdom`, `fast-check` (pruebas basadas en propiedades)
- **Backend:** RLS de PostgreSQL, `get_advisors` de Supabase (seguridad + performance)
- **Pagos:** MercadoPago sandbox (Checkout Pro + Connect)
- **CI/CD:** Vercel (build automático por push a `main`), `supabase functions deploy` manual
  para Edge Functions

## 9. Cómo reproducir las pruebas

```bash
cd frontend
npm install
npm run test    # suite de Vitest (componentes + propiedades)
npm run lint    # ESLint
npm run build   # build de producción
```

Para validar RLS y advisors tras un cambio de schema, se ejecuta `get_advisors` desde el
Dashboard de Supabase o vía la herramienta de desarrollo conectada al proyecto.
