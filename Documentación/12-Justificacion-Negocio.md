# Justificación de Negocio · Modo Ensayo

> **Versión:** 1.0 — 09-jul-2026

Este documento complementa `00-PRD.md` y `08-Justificacion-Tecnica.md`: reúne los objetivos,
la validación de mercado, los atributos de calidad y los requerimientos no funcionales, y
justifica que la solución construida resuelve las necesidades identificadas.

---

## 1. Objetivo general y específicos

**Objetivo general:** Desarrollar una plataforma digital que permita gestionar de manera
integral la organización de salas, clases, talleres y masterclass de danza y música,
conectando sedes, profesores y alumnos en un solo sistema, con el fin de ordenar la gestión
de actividades, mejorar la coordinación, aumentar la transparencia en los procesos y
optimizar la toma de decisiones.

**Objetivos específicos:**

| # | Objetivo específico | Coherencia con la problemática | Estado |
|---|---|---|---|
| OE1 | Gestionar el ciclo completo de una clase (usuarios, sedes, salas, publicación e inscripción) en un solo sistema centralizado, con roles y validación de identidad | Ataca la informalidad y el desorden de la gestión actual | ✅ Implementado |
| OE2 | Garantizar el pago al profesor mediante un mecanismo de pagos retenidos condicionados a la validación de la realización de la clase, con reagendamiento y devolución | Ataca la pérdida de ingresos por cancelaciones y la desconfianza | ✅ Implementado |

> El proyecto pide un objetivo general y **dos** objetivos específicos. Los OE fueron
> escogidos por su coherencia directa con la problemática. Las demás capacidades del sistema
> (gestión de salas/reservas, reputación, métricas por sede, arquitectura cloud) se
> desarrollaron como parte del alcance en servicio de estos dos objetivos.

---

## 2. Cliente y validación de mercado

El proyecto se dirige a un ecosistema de tres tipos de organizaciones/personas: **sedes**
(academias, salas de ensayo, centros culturales), **profesores independientes** de danza y
música, y **alumnos**. La validación de la necesidad se realizó mediante **encuestas
dirigidas a los tres perfiles** (profesores, alumnos y administradores de sede/academia),
complementadas con un análisis del estado del arte que comparó 7 soluciones existentes en el
mercado (Peerspace, The Rec Lab, Reservio, Jammed, Mindbody, Jackrabbit Dance, Anolla) y
determinó que ninguna resuelve de forma integral: validación de que la clase se realizó,
pagos retenidos condicionados a esa validación, reagendamiento formal y reputación de
usuarios especializada en el rubro de danza y música.

> **Nota:** [pendiente completar con el resultado numérico de las encuestas — cantidad de
> respuestas por perfil y hallazgos principales].

---

## 3. Necesidades del usuario → solución actual

Tabla de trazabilidad entre las necesidades identificadas por perfil de usuario y el
mecanismo de la plataforma que las resuelve hoy.

| Perfil | Necesidad | Solución en la plataforma |
|---|---|---|
| **Alumno** | Buscar clases por ciudad, comuna, disciplina u horario | Buscador con filtros combinados (`ClassesPage`) |
| **Alumno** | Inscribirse a sí mismo y a familiares/asociados en una misma compra | Carrito consolidado con beneficiarios (R07) |
| **Alumno** | Pagar de forma segura y saber qué pasa si la clase se suspende | Pago vía MercadoPago + pagos retenidos hasta validar la clase (R01) + reagendamiento con reembolso automático si se rechaza o vence el plazo |
| **Alumno** | Recibir notificaciones y saber el estado de sus clases | Campana de notificaciones accionables + panel "Mis Clases" |
| **Maestro independiente** | Encontrar y reservar salas adecuadas sin depender de un intermediario | Buscador de salas + reserva con pago directo, split automático a la sede (MercadoPago Connect) |
| **Maestro independiente** | Recibir sus pagos de forma ordenada | Pagos retenidos → liberados al confirmarse la clase → giro visible y gestionable en el panel de pagos del admin |
| **Maestro independiente** | Gestionar cupos, precios y horarios de sus clases | Creación/publicación de clases con capacidad, precio y horario propios |
| **Administrador de Sede** | Publicar sus salas y gestionar horarios | Registro de sede/sala con equipamiento, fotos y agenda de bloques horarios |
| **Administrador de Sede** | Validar si las clases se realizaron | Confirmación de clase (realizada/no realizada) desde el panel de la sede |
| **Administrador de Sede** | Ver ingresos y ocupación de sus salas | Dashboard con métricas de ocupación (M1) y panel de pagos con margen real de MercadoPago, desglosados por sede |
| **Maestro dependiente** | Ver sus clases asignadas y marcar asistencia | Vista de clases asignadas + flujo de asistencia (todos presentes por defecto, se desmarcan ausentes) |
| **Administrador General (interno)** | Aprobar sedes e identidades, controlar riesgos | Panel de administración: aprobación de sedes/identidades, gestión de roles, auditoría (`audit_logs`) |
| **Todos los perfiles** | Confianza en la plataforma | Sistema de reseñas/reputación tras clases completadas |

---

## 4. Atributos de calidad de la solución

| Atributo | Cómo se garantiza |
|---|---|
| **Integridad** | Transacciones ACID de PostgreSQL en operaciones de pago; constraints `CHECK` sobre los enums de estado; índice único que impide inscripciones duplicadas del mismo beneficiario en una clase |
| **Confiabilidad** | Row Level Security en el 100% de las tablas; triggers de auditoría de estado (`track_class_status`); tareas programadas (`pg_cron`) para timeouts y liberaciones automáticas |
| **Precisión** | Validación en dos capas: RLS/constraints en la base de datos + validación de negocio en las Edge Functions antes de cualquier escritura sensible |
| **Oportunidad** | Liberación de pagos inmediatamente al confirmarse la clase; notificaciones accionables; latido de disponibilidad cada 5 minutos para detectar caídas del servicio en tiempo casi real |
| **Seguridad** | Autenticación JWT vía Supabase Auth; autorización por rol en cada Edge Function sensible; RLS como última línea de defensa; nunca se expone la clave de servicio al cliente |

---

## 5. Requerimientos del sistema

### 5.1 Requerimientos funcionales (derivados de las 22 historias de usuario)

| ID | Requerimiento funcional |
|---|---|
| RF1 | Registro y autenticación de usuarios con roles (Alumno, Maestro, Admin de Sede, Admin General) |
| RF2 | Validación de identidad obligatoria para roles críticos |
| RF3 | Registro y publicación de sedes y salas con características y horarios |
| RF4 | Creación, publicación e inscripción de clases con cupos y precios |
| RF5 | Carrito con beneficiarios y pago con MercadoPago (retenido) |
| RF6 | Control de asistencia y confirmación de la realización de la clase |
| RF7 | Reagendamiento con notificación y decisión del alumno (plazo de 48 h) |
| RF8 | Sistema de reputación mediante reseñas post-clase |
| RF9 | Panel de administración: usuarios, sedes, pagos y métricas |

### 5.2 Requerimientos no funcionales

Los requerimientos no funcionales del sistema **se miden con las métricas del panel del
Administrador del Sistema (M1 a M5)**: son el instrumento con que efectivamente se mide el
desempeño del sistema en producción, calculadas en vivo sobre datos reales y desglosadas por
sede.

| Requerimiento | Objetivo | Métrica del administrador |
|---|---|---|
| Disponibilidad del servicio | > 95% | M4 Disponibilidad (latido interno `uptime_checks` + `pg_cron`) |
| Ocupación de salas | > 80% | M1 Ocupación (inscripciones activas / capacidad de sala) |
| Conversión de pago | > 70% | M2 Conversión (sesiones aprobadas / total de sesiones) |
| Tasa de asistencia | > 90% | M3 Asistencia (presentes / total de marcas) |
| Pagos exitosos | > 98% | M5 Pagos exitosos (aprobados / (aprobados + fallidos)) |
| Escalabilidad | Crecer sin rediseño | Infraestructura gestionada (Supabase + Vercel) |
| Seguridad de datos | Ningún acceso no autorizado | 100% de tablas con RLS, verificado con `get_advisors` |
| Calidad de código | Sin errores de lint ni de compilación | `npm run lint` (0 errores) y `npm run build` (exitoso) |

Estas cinco métricas (M1-M5) se muestran en vivo en el panel de administración, lo que
permite validar el cumplimiento de los requerimientos no funcionales de forma continua, no
solo en el momento de la entrega.

---

## 6. Conclusiones

El desarrollo de Modo Ensayo permitió construir una plataforma que cubre el ciclo completo
de una clase de danza o música — desde la reserva de la sala hasta la liberación del pago al
profesor — algo que ninguna de las soluciones del mercado analizadas resuelve de forma
integral. El equipo logró llevar el MVP a producción con las 22 historias de usuario
verificadas, 18 reglas de negocio implementadas, y funcionalidades adicionales no
contempladas en el alcance original: un panel de administración de pagos con margen real de
MercadoPago, métricas de rendimiento por sede, y analítica de comportamiento en vivo con
Google Analytics 4.

El principal aprendizaje del equipo fue la importancia de que la seguridad y las reglas de
negocio críticas vivan lo más cerca posible de los datos (Row Level Security, triggers,
tareas programadas), de modo que ningún error en una capa superior pueda comprometer la
integridad del sistema — un principio de defensa en profundidad que se aplicó de forma
consistente en toda la plataforma.
