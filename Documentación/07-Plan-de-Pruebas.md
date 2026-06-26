# Plan de Pruebas · Modo Ensayo

> **Versión:** 2.0 — 16-jun-2026 (Sprint 7 / inicio Sprint 9 QA)
> **Cobertura objetivo:** 60% (RNF-MAN-03)
> **Cobertura actual:** ≥ 60% — umbral JaCoCo activado, 70 tests pasan en CI

## 1. Objetivo

Asegurar que el MVP funcione correctamente bajo escenarios reales de uso, validando las 18 reglas de negocio, las 22 historias de usuario y los flujos críticos antes de la Evaluación Final Transversal.

## 2. Estrategia general

Tres niveles de pruebas complementarias:

| Nivel | Herramienta | Objetivo | Cobertura |
|---|---|---|---|
| **Unitarias** | JUnit 5 + Mockito (backend), Vitest (frontend) | Lógica de negocio aislada | 60% líneas |
| **Integración** | Spring Boot Test + TestContainers PostgreSQL | Flujos extremo-a-extremo backend | Flujos críticos |
| **Aceptación manual** | Plan de escenarios + lista de verificación | Validar HU completas en producción | 100% HU |

## 3. Ambiente de pruebas

### Backend
- Java 21 + Maven
- PostgreSQL 16 en contenedor Docker
- Spring Profile `test` con configuración aislada en `application-test.yml`
- Datos semilla específicos para tests reproducibles

### Frontend
- Node.js 22 + Vitest
- Mocks de axios para API
- jsdom para simular DOM

### Ambiente cloud para validación
- Backend desplegado en AWS ECS (sandbox)
- Frontend desplegado en S3 + CloudFront
- Base de datos RDS PostgreSQL
- MercadoPago en modo **sandbox**

## 4. Tests implementados — estado al 16-jun-2026

### Backend (10 archivos — 70 tests, 0 fallos)

| Archivo | Reglas / HU cubiertas | Casos |
|---|---|---|
| `AuthServiceTest.java` | HU01, HU02 | Registro válido, email duplicado, login OK, credenciales inválidas, usuario no encontrado |
| `UserServiceTest.java` | HU03, HU04, R21 | getProfile, identidad PENDING/APPROVED, updateProfile, changePassword, RUT inválido, documento duplicado, refundMethod, perfil profesional |
| `ClassServiceTest.java` | HU10, HU12, R08 | createBorrador sin identidad, borrador→publicar, conflicto horario, sede no aprobada, getById, listPublished |
| `AdminServiceTest.java` | HU21, R20, R22 | reviewIdentity (sin asignar TEACHER), approveVenue+VENUE_ADMIN, no duplicar rol, toggleUser con/sin motivo, assignRole |
| `PaymentServiceTest.java` | R01, R02, R10, R11, R12 | Checkout atómico, cupos llenos, duplicados, rollback |
| `ClassConfirmationServiceTest.java` | R01, R13 | REALIZADA libera pagos, NO_REALIZADA→REFUND_PENDING, ASIGNADA notifica a venue admin |
| `RescheduleServiceTest.java` | R15, R16, R17, R18 | Sugerencia de fechas, timeout 48h, decisión Maestro PROPIA/ASIGNADA |
| `ReviewServiceDemoTest.java` | HU20 | Crear reseña post-clase, elegibilidad |
| `AuthIntegrationTest.java` | HU01, HU02 | Registro+login E2E, contraseña corta, credenciales inválidas, acceso sin JWT |
| `AdminIntegrationTest.java` | HU21 | Flujo admin E2E con BD real |

### Frontend (4 archivos)

| Archivo | Funcionalidad |
|---|---|
| `CartPage.test.js` | Confirmación R14 antes de checkout, llamada a `createMercadoPagoPreference` |
| `PaymentSuccessPage.test.js` | Procesamiento de query params de MercadoPago |
| `ClassCard.test.js` | Renderizado de tarjeta de clase |
| `useAuth.test.js` | Composable de autenticación |

## 5. Plan de escenarios de aceptación

### 5.1 Escenarios del flujo crítico (P1)

**Escenario 1 — Registro y validación de identidad**
1. Usuario nuevo se registra (HU01)
2. Hace login (HU02)
3. Sube documento de identidad (HU03)
4. Admin General aprueba (HU21)
5. **Verificar:** `identidad_validada = true` en BD y banner desaparece

**Escenario 2 — Búsqueda, carrito y pago consolidado familiar (R07, R10, R11, R12, R14)**
1. Usuario crea 2 asociados (familiares)
2. Busca clase de "Danza Contemporánea" (HU05)
3. Agrega al carrito para sí mismo + asociado 1 + asociado 2 (HU07)
4. Hace checkout con confirmación (HU08)
5. Es redirigido a MercadoPago sandbox y completa pago
6. **Verificar:** 3 inscripciones creadas, 3 pagos en RETAINED, 1 consolidated_payment

**Escenario 3 — Reserva sala → rol Maestro → perfil profesional → publicar clase**
1. Alumno validado va a "Agendar tu Sala"
2. Reserva sala (DRAFT con room_id) (HU10)
3. **Verificar:** rol TEACHER asignado (R08), redirige a perfil profesional con primeraVez
4. Completa biografía + disciplina (HU04)
5. Va a "Clases por Asignar"
6. Elige opción "Crear Clase Nueva" (HU12) → publica
7. **Verificar:** clase en estado PUBLISHED visible en búsqueda

**Escenario 4 — Confirmación de clase realizada → liberación de pagos (R01, R13)**
1. Maestro marca asistencia el día de la clase
2. Admin Sede entra a "Clases por Confirmar" (HU17)
3. Confirma REALIZADA con diálogo
4. **Verificar:** todos los pagos asociados pasan a RELEASED, Maestro lo ve en su dashboard de earnings

**Escenario 5 — Confirmación NO_REALIZADA → devoluciones (R13)**
1. Admin Sede marca NO_REALIZADA
2. **Verificar:** todos los pagos pasan a REFUND_PENDING

**Escenario 6 — Reagendamiento con sugerencia + timeout 48h (R15, R16, R18)**
1. Maestro propone reagendamiento de clase PROPIA (HU18)
2. Sistema sugiere 3 fechas alternativas según agenda real
3. Maestro acepta una fecha → notificación a 4 alumnos
4. Alumno 1: acepta (R14) → su inscripción se mueve
5. Alumno 2: rechaza (R14) → su pago a REFUND_PENDING
6. Alumno 3: timeout 48h → su pago a REFUND_PENDING (R16)
7. Alumno 4: no recibe notificación porque ya estaba CANCELLED
8. **Verificar:** estado final correcto para los 4

**Escenario 7 — Reagendamiento ASIGNADA (R18)**
1. Maestro Dependiente intenta proponer reagendamiento → debe recibir HTTP 403
2. Solo Admin de Sede puede decidir (HU18)
3. **Verificar:** R18 enforced correctamente

**Escenario 8 — Cancelación de inscripción por Alumno (R14)**
1. Alumno ve "Mis Clases" (HU09)
2. Clica "Cancelar" en una clase futura
3. Diálogo con aviso de reembolso manual
4. Confirma
5. **Verificar:** enrollment a CANCELLED, pagos a REFUND_PENDING

### 5.2 Escenarios de borde y error (P2)

**Borde 1 — Identidad duplicada (R05)**
- Usuario A tiene RUT 19.831.314-9 APROBADO
- Usuario B intenta subir el mismo RUT → error "Documento ya verificado en otra cuenta"

**Borde 2 — Capacidad llena (R02)**
- Clase con capacidad 5, ya tiene 5 inscritos
- Usuario 6 intenta agregar al carrito → mensaje claro de capacidad llena

**Borde 3 — Pagos concurrentes para último cupo**
- 2 usuarios intentan pagar simultáneamente para el último cupo
- **Verificar:** solo uno tiene éxito, el otro recibe error y mantiene el dinero en su cuenta MP

**Borde 4 — Email duplicado en registro (HU01)**
- 2 personas intentan registrarse con el mismo email
- **Verificar:** HTTP 409 al segundo

**Borde 5 — Maestro sin perfil incompleto que intenta publicar**
- Maestro con perfil incompleto crea clase nueva
- **Verificar:** se puede publicar (no es bloqueante), pero ve banner de aviso

### 5.3 Escenarios de seguridad (P1)

**Seguridad 1 — Acceso sin JWT**
- GET `/api/users/me` sin Authorization → HTTP 401

**Seguridad 2 — JWT expirado**
- GET con JWT vencido → HTTP 401

**Seguridad 3 — Cancelación de inscripción ajena**
- Alumno A intenta cancelar inscripción del Alumno B → HTTP 403

**Seguridad 4 — Acción de Admin como usuario normal**
- Usuario común intenta `POST /api/admin/users/{id}/roles` → HTTP 403

**Seguridad 5 — Upload de archivos malicioso**
- Subir archivo `.exe` como documento de identidad → rechazado por tipo MIME

### 5.4 Pruebas de carga (P3)

**Carga 1 — 50 usuarios concurrentes navegando**
- 50 sesiones simulando búsqueda de clases en paralelo
- **Objetivo:** sin errores 500, tiempo respuesta < 1s p95

**Carga 2 — 20 checkouts simultáneos**
- 20 usuarios diferentes ejecutando checkout concurrente
- **Objetivo:** todos completan correctamente, sin condiciones de carrera

**Carga 3 — Listado de clases con BD poblada (1000 clases)**
- BD con 1000 clases publicadas + filtros
- **Objetivo:** consulta < 500ms p95

## 5.5 Lista de verificación aceptación — 22 HU

| HU | Descripción | Estado manual | Sprint |
|---|---|---|---|
| HU01 | Registro con correo único + RUT único | ✓ Verificado | S1 |
| HU02 | Login JWT + manejo de credenciales inválidas | ✓ Verificado | S1 |
| HU03 | Upload documento identidad + validación RUT + unicidad | ✓ Verificado | S2 |
| HU04 | Perfil profesional Maestro (biografía + disciplinas + redes) | ✓ Verificado | S4 |
| HU05 | Búsqueda de clases con filtros simultáneos (AND) | ✓ Verificado | S3 |
| HU06 | Ver detalle de clase con cupos disponibles | ✓ Verificado | S3 |
| HU07 | Carrito con beneficiarios, validar duplicados | ✓ Verificado | S3 |
| HU08 | Checkout consolidado MercadoPago + estado RETAINED | ✓ Verificado | S4 |
| HU09 | Cancelar inscripción → REFUND_PENDING | ✓ Verificado | S5 |
| HU10 | Reservar sala → asignar rol TEACHER en primera publicación | ✓ Verificado | S3 |
| HU11 | Crear borrador sin sala → asignar sala después | ✓ Verificado | S3 |
| HU12 | Publicar clase (DRAFT → PUBLISHED) con validaciones | ✓ Verificado | S3 |
| HU13 | Marcar asistencia ±2 horas de la clase | ✓ Verificado | S5 |
| HU14 | Dashboard Alumno: mis clases + estados | ✓ Verificado | S5 |
| HU15 | Registrar sede SEDE / HOME_STUDIO con documentos SII | ✓ Verificado | S2/S8 |
| HU16 | Registrar sala con todos los campos de equipamiento | ✓ Verificado | S2 |
| HU17 | Admin Sede confirma REALIZADA / NO_REALIZADA | ✓ Verificado | S5 |
| HU18 | Reagendamiento R19 + sugerencias + decisión Alumno 48h | ✓ Verificado | S6 |
| HU19 | Panel Admin General: usuarios, sedes, métricas | ○ Pendiente Sprint 9 | S7 |
| HU20 | Dejar reseña post-clase completada | ○ Pendiente Sprint 9 | S8 |
| HU21 | Admin General aprueba identidad / sede (sin asignar roles indebidos) | ✓ Verificado | S7 |
| HU22 | Botones de contexto [Alumno][Profesor][Mi Sede] con movilidad de perfil | ○ Pendiente Sprint 9 | S7 |

## 6. Métricas de aceptación

| Métrica | Objetivo | Estado actual |
|---|---|---|
| Cobertura backend (JaCoCo) | ≥ 60% | **≥ 60% — umbral activo, 70 tests** |
| Cobertura frontend (Vitest) | ≥ 50% | ~25% (pendiente Sprint 9) |
| Tiempo respuesta GET /classes | < 500ms p95 | Optimizado con JOIN FETCH + Caffeine cache |
| Tiempo respuesta POST /checkout | < 2s p95 | Por medir con JMeter |
| Bugs críticos al cierre | 0 | 0 bugs críticos conocidos |
| Bugs menores al cierre | ≤ 3 | Por medir Sprint 9 |
| HU verificadas manualmente | 22 / 22 | **19 / 22** (HU19, HU20, HU22 pendientes Sprint 9) |

## 7. Responsabilidades

| Área | Responsable | Estado |
|---|---|---|
| Tests unitarios backend | Jonathan | ✓ 10 archivos, 70 tests, JaCoCo ≥ 60% |
| Tests unitarios frontend | Victor | Pendiente ampliar a 50% (4 archivos actuales) |
| Tests de integración | Jonathan + Darlette | 2 archivos (Auth + Admin) — ampliar con ClassFlow |
| Aceptación manual HU19, HU20, HU22 | Equipo completo | Sprint 9 |
| Pruebas de carga JMeter 50 usuarios | Darlette | Plan JMeter creado — ejecutar en Sprint 9 |
| Revisión de seguridad | Jonathan | Pendiente Sprint 9 |

## 8. Herramientas

- **Backend:** JUnit 5, Mockito, AssertJ, Spring Boot Test, TestContainers
- **Frontend:** Vitest, jsdom, @testing-library/vue
- **Cobertura:** JaCoCo (backend), Vitest coverage (frontend)
- **Carga:** Apache JMeter o k6
- **CI:** GitHub Actions ejecuta tests en cada PR

## 9. Plan de ejecución por sprint

| Sprint | Foco de pruebas |
|---|---|
| 5 (actual) | Validar Escenarios 4 y 5 del flujo crítico |
| 6 | Escenarios 6 y 7 (reagendamiento) |
| 7 | Escenarios de seguridad y permisos |
| 8 | Escenarios de borde + accesibilidad móvil |
| 9 | **QA integral:** todos los escenarios + cobertura objetivo + carga |
| 10 | Bugs residuales + validación final |
