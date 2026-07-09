# Justificación de Decisiones Técnicas · Modo Ensayo

> **Versión:** 2.0 — 09-jul-2026

Este documento explica el "por qué" detrás de cada decisión técnica y de arquitectura del
proyecto. La intención es demostrar que las elecciones son fundamentadas, no arbitrarias.

---

## 1. Stack tecnológico

### 1.1 Frontend: Vue 3 + Vite + Tailwind CSS

**¿Por qué Vue 3 y no React/Angular?**

| Criterio | Vue 3 | React | Angular |
|---|---|---|---|
| Curva de aprendizaje | Baja (HTML/CSS amigable) | Media (JSX) | Alta (TypeScript + RxJS) |
| Tiempo de boot del proyecto | Rápido | Rápido | Más config |
| SFC (Single File Component) | ✓ (claros, separación natural) | Mezcla JSX | ✓ pero más verboso |
| Reactividad implícita | ✓ (`ref`, `reactive`, `computed`) | useState/useEffect | RxJS observables |
| Tamaño bundle base | ~33 KB gzip | ~42 KB gzip | ~150 KB gzip |
| Equipo familiarizado | ✓ | Parcial | No |

**Decisión:** Vue 3 con Composition API por su curva más amigable, reactividad explícita y
tamaño de bundle reducido.

**¿Por qué Vite y no Webpack/Vue CLI?**
- Vite usa ESM nativo en dev → HMR instantáneo.
- Build de producción con Rollup → bundles más pequeños.
- Configuración mínima (`vite.config.js` < 20 líneas).

**¿Por qué Tailwind CSS y no Bootstrap/CSS Modules?**
- Utility-first → consistencia visual sin clases personalizadas.
- Tree-shaking automático → bundle final solo con clases usadas (~10 KB gzip).
- Adapta el design system del equipo (colores, espaciados) en una sola fuente: `tailwind.config.js`.

---

### 1.2 Backend: Supabase (PostgreSQL + Auth + Storage + Edge Functions)

**¿Por qué Supabase (BaaS) y no un backend propio (Node/Django/FastAPI con servidor dedicado)?**

| Criterio | Supabase | Backend propio (Node/Django) |
|---|---|---|
| Auth con roles y JWT | ✓ nativo (Supabase Auth) | Hay que construirlo (Passport/JWT manual) |
| Autorización a nivel de fila | ✓ RLS declarativo en SQL | Manual en cada endpoint |
| API autogenerada (PostgREST) | ✓ sobre las tablas | Hay que escribir cada endpoint CRUD |
| Tareas programadas | ✓ `pg_cron` nativo | node-cron / Celery (terceros) |
| Almacenamiento de archivos | ✓ Storage integrado con RLS | S3 + lógica de permisos aparte |
| Tiempo de setup para equipo de 3 en 11 semanas | Bajo (sin servidor que mantener) | Alto (infra + auth + storage desde cero) |
| Escalado | Gestionado por la plataforma | Requiere configurar orquestación |

**Decisión:** Supabase por:
- **Row Level Security (RLS):** la autorización vive en la base de datos, no repartida en
  cada endpoint — es la **última línea de defensa** ante bugs de lógica de negocio.
- **Edge Functions (Deno + TypeScript):** lógica de negocio sensible (pagos, transiciones de
  estado, acciones de admin) corre con la clave de servicio, fuera del alcance del cliente.
- **`pg_cron`:** habilita tareas programadas (liberar bloques expirados, timeout de
  reagendamiento de 48h, latido de disponibilidad) sin infraestructura adicional.
- **Equipo de 3 personas, 11 semanas:** un backend propio hubiera consumido tiempo de
  desarrollo en infraestructura (auth, storage, cron) en vez de en funcionalidad de negocio.

---

### 1.3 Base de datos: PostgreSQL 16 (gestionado por Supabase)

**¿Por qué PostgreSQL y no MySQL/MongoDB?**

| Criterio | PostgreSQL | MySQL | MongoDB |
|---|---|---|---|
| Soporte de constraints CHECK | ✓ | Limitado (8.0+) | No (validators) |
| Triggers complejos | ✓ Robustos | Limitados | No nativos |
| JSON nativo (JSONB) | ✓ Indexable | JSON (lento) | Nativo pero sin schemas |
| UUID nativo | ✓ `gen_random_uuid()` | Sólo VARCHAR | ObjectId |
| Procedimientos almacenados | ✓ PL/pgSQL maduro | OK | No |
| Row Level Security nativo | ✓ | No | No |
| Transacciones ACID | ✓ | ✓ (InnoDB) | Limitadas |

**Decisión:** PostgreSQL 16 por:
- **Triggers de negocio** críticos: `track_class_status`, `enforce_class_capacity`, cálculo
  automático de estados de pago.
- **CHECK constraints** sobre enums (estados de clase, de pago, de reserva).
- **UUID v4** como PK (mejor para sistemas distribuidos sin coordinación de IDs).
- **JSONB indexable** para `cart_snapshot`, `metadata` de auditoría.
- **Row Level Security nativo**, la razón principal de elegir Supabase como plataforma.

**¿Por qué tener lógica en triggers y funciones de BD?**
- Es la **última línea de defensa**. Si un bug del frontend o de una Edge Function permite
  una operación inválida, la base de datos la rechaza.
- Reglas críticas (ej. capacidad de clase, transiciones de estado) quedan garantizadas
  incluso si se modifica el código del cliente.

---

### 1.4 Pagos: MercadoPago Checkout Pro + Connect (marketplace)

**¿Por qué MercadoPago y no Stripe/Khipu/PayPal?**

| Criterio | MercadoPago | Stripe | Khipu | PayPal |
|---|---|---|---|---|
| Mercado chileno (target) | ✓ Líder | Limitado | ✓ | ✓ |
| Sandbox completo | ✓ | ✓ | Limitado | ✓ |
| SDK/API REST bien documentada | ✓ | ✓ | No | ✓ |
| Webhooks robustos | ✓ | ✓ | Básicos | ✓ |
| Marketplace con split de pagos | ✓ Connect | ✓ Connect | No | Limitado |
| Comisiones razonables | ~4% | ~3.6% | ~1.5% | ~4% |
| Documentación en español | ✓ | Parcial | ✓ | ✓ |

**Decisión:** MercadoPago por:
- **Mercado objetivo:** plataforma chilena para usuarios chilenos.
- **Sandbox real:** permite pruebas extremo a extremo sin costo.
- **Checkout Pro** para inscripción a clases (pago simple) y **Connect** para arriendo de
  salas, donde el dinero va directo a la cuenta de la sede con una comisión de plataforma
  (`marketplace_fee`) — la plataforma nunca custodia el dinero de terceros.
- **Integración real** (no simulada): en producción solo cambian las credenciales de TEST a
  producción.

---

### 1.5 Hosting: Vercel (frontend) + Supabase (backend gestionado)

**¿Por qué Vercel y no un servidor propio / S3+CloudFront administrado a mano?**

| Criterio | Vercel | Servidor propio |
|---|---|---|
| CI/CD automático por push a `main` | ✓ | Hay que configurarlo |
| CDN global | ✓ nativo | Requiere configuración aparte |
| HTTPS automático | ✓ | Requiere certificado manual |
| Variables de entorno por ambiente | ✓ | Manual |
| Costo para el volumen del proyecto | Gratis (plan Hobby) | Costo de servidor + mantención |

**Decisión:** Vercel + Supabase por:
- **Sin servidores que administrar:** ni el frontend ni el backend requieren mantención de
  infraestructura por parte del equipo.
- **Costo:** gratuito en desarrollo (plan Free de Supabase + Hobby de Vercel), ~US$25/mes en
  producción (Supabase Pro) — apropiado para el volumen de tráfico esperado del MVP.
- **Deploy automático:** cada push a `main` publica el frontend; las Edge Functions se
  despliegan con `supabase functions deploy`.

---

## 2. Decisiones de arquitectura

### 2.1 Monolito modular (no microservicios)

**Decisión:** Un solo backend (Supabase) organizado por dominios (auth, classes, payments,
reschedules, venues, admin), con Edge Functions independientes por operación de negocio.

**Justificación:**
- Equipo de 3 personas en 11 semanas: la complejidad operativa de microservicios (orquestación,
  comunicación entre servicios, consistencia distribuida) supera el beneficio para este alcance.
- La **transaccionalidad del checkout** es crítica y se resuelve de forma simple con
  transacciones de PostgreSQL, sin necesidad de patrones de coordinación distribuida (saga).
- Cada Edge Function es una unidad de despliegue independiente (se puede desplegar una sola
  sin afectar las demás), lo que da parte de la flexibilidad de microservicios sin su costo
  operativo.

### 2.2 Servicios por dominio en el frontend (Package by Feature)

**Decisión:** `frontend/src/services/` tiene un módulo por dominio (`classService.js`,
`venueService.js`, `paymentService.js`, etc.), cada uno con sus propias operaciones.

**Justificación:**
- Cuando un integrante trabaja en una funcionalidad, encuentra todo lo relacionado a ese
  dominio en un solo archivo.
- Cohesión alta dentro del dominio, acoplamiento bajo entre dominios.

### 2.3 RLS como capa de autorización (no lógica repartida en el cliente)

**Decisión:** Cada tabla tiene Row Level Security; el frontend usa la clave pública
(anon/publishable) y solo puede leer/escribir lo que las políticas permiten.

**Justificación:**
- **Seguridad:** un usuario no puede ver ni modificar datos de otro aunque manipule las
  peticiones desde el navegador — la restricción está en la base de datos, no en el cliente.
- **Estabilidad:** las políticas no dependen de que cada vista del frontend "recuerde" filtrar
  correctamente.

### 2.4 Triggers y funciones de BD para reglas críticas

**Decisión:** Reglas como control de capacidad de clase, seguimiento de estados y liberación
de bloques de horario expirados están implementadas como triggers y funciones de PostgreSQL.

**Justificación:**
- Defensa en profundidad: incluso si un bug pasa por el frontend o una Edge Function, la BD
  rechaza la operación inválida.
- No tienen overhead notorio y son confiables.

### 2.5 JWT stateless (Supabase Auth)

**Decisión:** Autenticación 100% stateless con JWT emitido por Supabase Auth, sin sesión en
servidor propio.

**Justificación:**
- Escalabilidad horizontal sin necesidad de sesiones compartidas.
- Compatible con una SPA (frontend almacena el token y lo refresca automáticamente).

**Trade-off conocido:** invalidar un JWT antes de su expiración requiere revocación explícita
(ban de usuario) en vez de una simple blacklist local; se acepta porque el token dura 24h y
el usuario puede cerrar sesión limpiando el almacenamiento local.

---

## 3. Decisiones de UX

### 3.1 Modos de contexto (Alumno / Maestro / Sede / Admin)

**Decisión:** En lugar de portales separados, un solo dashboard con switcher de "contexto
activo" que cambia la navegación y las acciones disponibles.

**Justificación:**
- Un usuario puede ser Alumno + Maestro + Admin de Sede simultáneamente.
- Forzar cambio de URL/login entre contextos rompería la experiencia.
- El sistema persiste el contexto activo por usuario para recordar la última vista.

### 3.2 Confirmación explícita en acciones irreversibles

**Decisión:** Componentes de confirmación (modales) reutilizables antes de cualquier acción
no recuperable (pago, cancelación, rechazo de reagendamiento).

**Justificación:**
- Reduce errores costosos.
- Patrón consistente que el usuario aprende una vez y reconoce en toda la plataforma.

### 3.3 Banners persistentes sobre estado incompleto

**Decisión:** Cuando un Maestro no tiene su perfil profesional completo (o falta cuenta de
MercadoPago), un banner visible en todo el contexto Maestro lo recuerda y permite completarlo
en un clic.

**Justificación:**
- Un perfil incompleto lo hace invisible para los alumnos → conversión cero.
- El banner no bloquea (no es un diálogo modal), persiste como recordatorio y desaparece
  automáticamente al cumplirse los requisitos.

---

## 4. Coherencia entre problemática, solución y tecnologías

### Problemática
> *Los profesores freelance de artes escénicas pierden ingresos por cancelaciones de última
> hora, y la organización de clases se maneja de forma informal (WhatsApp, papel,
> transferencias sin control), sin garantía de pago para el profesor ni trazabilidad para el
> alumno o la sede.*

### Solución
Modo Ensayo introduce **pagos retenidos condicionados a la realización efectiva de la
clase**, confirmados por la sede, con un mecanismo de reagendamiento orquestado para
minimizar pérdidas tanto para profesores como para alumnos.

### Cómo las tecnologías habilitan la solución

| Necesidad del producto | Tecnología que la habilita |
|---|---|
| Garantía de atomicidad en el checkout | Transacciones ACID de PostgreSQL |
| Liberación automática de pagos al confirmar la clase | Trigger + función de PostgreSQL |
| Timeout de 48h para la decisión del alumno en un reagendamiento | `pg_cron` |
| Confirmación explícita en cada decisión | Componente Vue + validación en RLS/Edge Function |
| Pago real con sandbox para pruebas | MercadoPago Checkout Pro + Connect |
| Múltiples roles simultáneos por usuario | `app_metadata.roles` en el JWT de Supabase Auth |
| Auditoría completa de cambios | Tabla `audit_logs` + triggers de PostgreSQL |
| Split automático del arriendo de sala a la cuenta correcta | MercadoPago Connect (OAuth) + `marketplace_fee` |
| Disponibilidad medible sin depender de servicios externos | Latido interno (`pg_cron` + tabla `uptime_checks`) |
| Métricas de uso reales | Google Analytics 4 vía Edge Function con service account |

---

## 5. Decisiones que se evaluaron y descartaron

| Opción descartada | Razón |
|---|---|
| Microservicios | Equipo pequeño + plazo corto; la complejidad operativa no se compensa para este alcance |
| Backend propio (Node/Django) en vez de Supabase | Reconstruir auth, RLS-equivalente y cron consumiría tiempo de desarrollo sin aportar valor de negocio |
| GraphQL | Curva de aprendizaje no justificada para el alcance |
| WebSockets para notificaciones | Polling periódico es suficiente y más simple de mantener |
| Redis para caché | El tráfico esperado no lo requiere; PostgreSQL alcanza |
| Stripe en lugar de MercadoPago | El mercado chileno usa MercadoPago mayoritariamente |
| Next.js/Nuxt en lugar de SPA pura | SEO no es prioridad del MVP (la búsqueda interna sí) |
| Firebase Auth | Se prefirió mantener el control del flujo de validación de identidad dentro de Supabase Auth |

---

## 6. Decisiones futuras (post-MVP)

| Necesidad futura | Decisión propuesta |
|---|---|
| Notificaciones push | Firebase Cloud Messaging |
| Email transaccionales | Proveedor de email transaccional (ej. Resend) |
| Caché agresivo | Redis si el tráfico supera niveles actuales |
| Búsqueda full-text avanzada | `pg_trgm` o Elasticsearch |
| Desembolso automático a profesores | Integración de money-out cuando MercadoPago Chile lo habilite |

Estas decisiones no son parte del MVP pero quedan documentadas como roadmap.
