# Justificación de Decisiones Técnicas · Modo Ensayo

> **Versión:** 1.0 — 30-may-2026

Este documento explica el "por qué" detrás de cada decisión técnica y de arquitectura del proyecto. La intención es demostrar que las elecciones son fundamentadas, no arbitrarias.

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
| Equipo familiarizado | ✓ Victor | Parcial | No |

**Decisión:** Vue 3 con Composition API por su curva más amigable, reactividad explícita y tamaño de bundle reducido. El equipo (Victor) ya tenía experiencia previa.

**¿Por qué Vite y no Webpack/Vue CLI?**
- Vite usa ESM nativo en dev → HMR instantáneo.
- Build de producción con Rollup → bundles más pequeños.
- Configuración mínima (`vite.config.js` < 20 líneas).

**¿Por qué Tailwind CSS y no Bootstrap/CSS Modules?**
- Utility-first → consistencia visual sin clases personalizadas.
- Tree-shaking automático → bundle final solo con clases usadas (~10 KB gzip).
- Adapta el design system del equipo (colores, espaciados) en una sola fuente: `tailwind.config.js`.

---

### 1.2 Backend: Spring Boot 3.2 + Java 21

**¿Por qué Spring Boot y no Node.js/Django/FastAPI?**

| Criterio | Spring Boot | Node.js (Express/Nest) | Django | FastAPI |
|---|---|---|---|---|
| Tipado fuerte en runtime | ✓ (Java) | Solo con TypeScript estricto | Limitado | ✓ (Pydantic) |
| Madurez del ecosistema | Alta | Alta | Alta | Media |
| ORM robusto | ✓ JPA/Hibernate | TypeORM/Prisma | ORM nativo | SQLAlchemy |
| Manejo de transacciones | ✓ Declarativo `@Transactional` | Manual | ✓ Decoradores | Manual |
| Seguridad nativa | ✓ Spring Security | Passport.js (terceros) | Django Auth | OAuth2 (terceros) |
| Scheduled tasks | ✓ `@Scheduled` nativo | node-cron (terceros) | Celery | Background tasks |
| Soporte académico | Asignatura cubre Java | No cubierto | No cubierto | No cubierto |

**Decisión:** Spring Boot por:
- **Cumplimiento curricular:** la asignatura usa Java; aprovechamos la base aprendida.
- **`@Transactional`:** crítico para la **atomicidad del checkout (R11)** sin bibliotecas externas.
- **`@Scheduled`:** clave para implementar el **timeout 48h de reagendamiento (R16)** sin terceros.
- **Spring Security:** JWT y manejo de roles maduro.

**¿Por qué Java 21 (no 17 o 11)?**
- LTS vigente al inicio del proyecto.
- Features modernas: `record`, `pattern matching`, virtual threads.
- Compatible con Spring Boot 3.2 (recomendación oficial).

---

### 1.3 Base de datos: PostgreSQL 16

**¿Por qué PostgreSQL y no MySQL/MongoDB?**

| Criterio | PostgreSQL | MySQL | MongoDB |
|---|---|---|---|
| Soporte de constraints CHECK | ✓ | Limitado (8.0+) | No (validators) |
| Triggers complejos | ✓ Robustos | Limitados | No nativos |
| JSON nativo (JSONB) | ✓ Indexable | JSON (lento) | Nativo pero sin schemas |
| UUID nativo | ✓ `uuid_generate_v4()` | Sólo VARCHAR | ObjectId |
| Procedimientos almacenados | ✓ PL/pgSQL maduro | OK | No |
| Transacciones ACID | ✓ | ✓ (InnoDB) | Limitadas |

**Decisión:** PostgreSQL 16 por:
- **Triggers de negocio** críticos: `trg_release_payment`, `trg_check_capacity`, `trg_class_status_change`.
- **CHECK constraints** sobre enums (R02, R13).
- **UUID v4** como PK (mejor para sistemas distribuidos sin coordinación de IDs).
- **JSONB indexable** para `notifications.data` y `refund_methods.details`.

**¿Por qué tener lógica en triggers de BD?**
- Es la **última línea de defensa**. Si un bug del backend permite un INSERT inválido, la BD lo rechaza.
- La liberación automática de pagos (`trg_release_payment`) garantiza R01 incluso si se modifica el código del servicio.

---

### 1.4 Pagos: MercadoPago Checkout Pro

**¿Por qué MercadoPago y no Stripe/Khipu/PayPal?**

| Criterio | MercadoPago | Stripe | Khipu | PayPal |
|---|---|---|---|---|
| Mercado chileno (target) | ✓ Líder | Limitado | ✓ | ✓ |
| Sandbox completo | ✓ | ✓ | Limitado | ✓ |
| SDK Java oficial | ✓ | ✓ | No | ✓ |
| Webhooks robustos | ✓ | ✓ | Básicos | ✓ |
| Comisiones razonables | ~4% | ~3.6% | ~1.5% | ~4% |
| Documentación en español | ✓ | Parcial | ✓ | ✓ |

**Decisión:** MercadoPago Checkout Pro por:
- **Mercado objetivo:** plataforma chilena para usuarios chilenos.
- **Sandbox real:** permite pruebas extremo a extremo sin costo.
- **SDK Java oficial:** integración directa con backend Spring Boot.
- **Pago consolidado** soportado nativamente (un solo cobro con múltiples items).

**No fue simulado:** La integración con MercadoPago es **real** (no mocked). En producción solo se cambian las credenciales de TEST a producción.

---

## 2. Decisiones de arquitectura

### 2.1 Monolito modular vs Microservicios

**Decisión:** Monolito modular con 6 dominios (auth, classes, payments, reschedules, users, venues, admin, etc.)

**Justificación:**
- Equipo de 3 personas en 11 semanas: complejidad operativa de microservicios (k8s, service mesh, mensajería) supera el beneficio para este alcance.
- La **transaccionalidad del checkout (R11)** es crítica y trivial en monolito (`@Transactional` Spring), difícil en microservicios (saga pattern).
- Migración futura a microservicios es viable porque cada dominio ya está aislado en su propio package.

### 2.2 Package by Feature (no by Layer)

**Decisión:** Cada dominio (`payments`, `classes`, etc.) tiene su propia estructura completa: controller, service, repository, domain, dto.

**Justificación:**
- Cuando un integrante trabaja en una feature, no salta entre 5 carpetas (`controllers/`, `services/`, `models/`...).
- Cohesión alta dentro del dominio, acoplamiento bajo entre dominios.
- Refactor de un dominio es local.

### 2.3 DTOs obligatorios (nunca exponer entidades JPA)

**Decisión:** Todos los endpoints retornan DTOs específicos (`UserProfileResponse`, `ClassResponse`, etc.), no entidades.

**Justificación:**
- **Seguridad:** evita exponer accidentalmente campos sensibles (`password_hash`).
- **Estabilidad de API:** cambios en entidades no rompen contratos REST.
- **Optimización:** los DTOs traen solo lo necesario, evitando N+1 en `@OneToMany`.

### 2.4 Triggers de BD para reglas críticas

**Decisión:** R01 (liberación de pagos), R02 (capacidad), R03 (auditoría) están implementadas también como triggers.

**Justificación:**
- Defensa en profundidad: incluso si un bug pasa por el servicio, la BD rechaza la operación inválida.
- Triggers de PostgreSQL son confiables y no tienen overhead notorio.
- La auditoría histórica no se puede "olvidar" en código de aplicación.

### 2.5 JWT stateless

**Decisión:** Autenticación 100% stateless con JWT firmado HMAC256, sin sesión en backend.

**Justificación:**
- Escalabilidad horizontal sin sticky sessions.
- Despliegue en ECS Fargate con N instancias trivial.
- Compatible con SPAs (frontend almacena en localStorage).

**Trade-off conocido:** invalidar un JWT antes de su expiración requiere blacklist en BD. Lo aceptamos porque el JWT dura 24h y los usuarios pueden cerrar sesión limpiando localStorage.

### 2.6 Auto-sync de atributos vía interceptor Axios

**Decisión:** El backend retorna `atributosActualizados: true` en respuestas relevantes; el frontend tiene un interceptor que dispara `syncAtributos()` automáticamente.

**Justificación:**
- Evita que el frontend tenga que conocer en qué endpoints cambia el estado del usuario.
- Mantiene el store sincronizado con BD sin lógica manual repetitiva.
- Permite que la asignación dinámica del rol TEACHER (R08) se refleje inmediatamente en la UI.

---

## 3. Decisiones de infraestructura

### 3.1 Docker Compose para desarrollo local

**Decisión:** Un solo `docker-compose up -d` levanta Postgres + Backend + Frontend + pgAdmin.

**Justificación:**
- Onboarding de 5 minutos para cualquier integrante.
- Ambiente idéntico en todas las máquinas (Windows, macOS, Linux).
- Aisla dependencias del sistema.

### 3.2 AWS ECS Fargate (no EC2/Beanstalk)

**Decisión:** ECS Fargate gestionado, sin instancias EC2 que mantener.

**Justificación:**
- **Sin gestión de servidores:** AWS administra el cluster.
- **Pago por uso:** ideal para tráfico bajo durante el proyecto.
- **Escala automática:** definida en task definition.
- **CI/CD trivial:** push de imagen Docker a ECR + actualización de service.

### 3.3 RDS PostgreSQL en subnet privada

**Decisión:** RDS con multi-AZ deshabilitado (costo), en subnet privada, accesible solo desde ECS.

**Justificación:**
- **Sin acceso público a BD:** se accede solo vía bastion host SSH o el propio backend.
- **Backups automáticos:** RDS hace snapshots diarios sin código adicional.

### 3.4 S3 + CloudFront para frontend

**Decisión:** Frontend estático servido por CloudFront, con S3 como origen.

**Justificación:**
- CDN global → latencia mínima desde cualquier país.
- HTTPS automático con certificado ACM.
- Sin servidor que mantener para servir archivos estáticos.

---

## 4. Decisiones de UX

### 4.1 Modos de contexto (Alumno / Maestro / Sede / Admin)

**Decisión:** En lugar de portales separados, un solo dashboard con switcher de "contexto activo" que cambia la navbar y las acciones disponibles.

**Justificación:**
- Un usuario puede ser Alumno + Maestro + Admin de Sede simultáneamente.
- Forzar cambio de URL/login entre contextos rompería la experiencia.
- El sistema persiste `modoActual` por usuario para recordar la última vista.

### 4.2 Confirmación explícita en cada acción irreversible (R14)

**Decisión:** Componente `ConfirmModal.vue` reutilizable que se invoca antes de cualquier acción no recuperable.

**Justificación:**
- Reduce errores costosos (pago accidental, cancelación errónea).
- Cumple R14 sin duplicar código en cada vista.
- Patrón consistente que el usuario aprende.

### 4.3 Banners persistentes sobre estado incompleto

**Decisión:** Cuando el Maestro no tiene perfil profesional completo, un banner amarillo visible en TODO el contexto Maestro lo recuerda y permite completarlo en 1 click.

**Justificación:**
- Maestros con perfil incompleto son invisibles para alumnos → conversión cero.
- El banner no bloquea (no es un dialog modal) sino que persiste como reminder.
- Desaparece automáticamente al cumplir los mínimos.

---

## 5. Coherencia entre problemática, solución y tecnologías

### Problemática
> *Los Maestros freelance de artes pierden ingresos por cancelaciones de última hora, y los Alumnos tienen pocas plataformas confiables para reservar clases. Las plataformas existentes no garantizan al Maestro su pago hasta que la clase se realice.*

### Solución
Modo Ensayo introduce **pagos retenidos condicionados a la realización efectiva de la clase** (R01), confirmados por una tercera parte (Admin de Sede), con un mecanismo de reagendamiento orquestado (R15-R18) para minimizar pérdidas tanto para Maestros como Alumnos.

### Cómo las tecnologías habilitan la solución

| Necesidad del producto | Tecnología que la habilita |
|---|---|
| Garantía de atomicidad en checkout (R11) | Spring `@Transactional` + PostgreSQL ACID |
| Liberación automática de pagos al confirmar clase (R01) | Trigger `trg_release_payment` en PostgreSQL |
| Timeout 48h para decisión del Alumno (R16) | Spring `@Scheduled` + `@EnableScheduling` |
| Confirmación explícita en cada decisión (R14) | Componente Vue + validación backend |
| Pago real con sandbox para pruebas | MercadoPago Checkout Pro + SDK Java |
| Múltiples roles simultáneos por usuario (R08) | Many-to-many `user_roles` + JWT con array de roles |
| Auditoría completa de cambios | Triggers de PostgreSQL escribiendo en tablas históricas |
| Despliegue público para evaluación | AWS ECS Fargate + RDS + CloudFront |

---

## 6. Decisiones que se evaluaron y descartaron

| Opción descartada | Razón |
|---|---|
| Microservicios | Equipo pequeño + plazo corto, complejidad operativa no se compensa |
| GraphQL | Curva de aprendizaje no justificada para el alcance |
| WebSockets para notificaciones | Polling cada 30s es suficiente y más simple |
| Redis para caché | Tráfico esperado no lo requiere; PostgreSQL alcanza |
| Stripe en lugar de MercadoPago | Mercado chileno usa MP mayoritariamente |
| Next.js / Nuxt en lugar de SPA pura | SEO no es prioridad del MVP (búsqueda interna sí) |
| Firebase Auth | Pierde control sobre el flujo de validación de identidad personalizado |

---

## 7. Decisiones futuras (post-MVP)

| Necesidad futura | Decisión propuesta |
|---|---|
| Notificaciones push | Firebase Cloud Messaging |
| Email transaccionales | AWS SES |
| Analytics de uso | PostHog (self-hosted) o Plausible |
| Caché agresivo | Redis si tráfico supera 1000 req/s |
| Búsqueda full-text avanzada | Elasticsearch o pg_trgm |
| Backup off-site | Snapshots RDS replicados a S3 cross-region |

Estas decisiones no son parte del MVP pero quedan documentadas como roadmap.
