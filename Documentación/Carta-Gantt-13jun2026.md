# Carta Gantt — Modo Ensayo
## Actualizada al 09 de julio de 2026 (previo a Evaluación Final)

**Equipo:** Darlette Morales · Jonathan Guerra · Victor Silva
**Asignatura:** Taller Aplicado de Programación - TPY1101
**Profesor:** Felipe Arturo Castillo Ducaud

---

## 1. Estado General

| Sprint | Semana | Foco | Estado |
|--------|--------|------|--------|
| S0 | 22-25 abr | Documentación de diseño | ✅ COMPLETADO |
| S1 | 27-abr/3-may | Setup + Auth | ✅ COMPLETADO |
| S2 | 4-10 may | Usuarios + Sedes + Docs | ✅ COMPLETADO |
| S3 | 11-17 may | Clases + Búsqueda + Carrito | ✅ COMPLETADO |
| S4 | 18-24 may | Pago consolidado | ✅ COMPLETADO |
| S5 | 25-31 may | Validación de clases + liberación de pagos | ✅ COMPLETADO |
| S6 | 1-7 jun | Reagendamiento + timeout 48h | ✅ COMPLETADO |
| S7 | 8-14 jun | Admin general + notificaciones | ✅ COMPLETADO |
| S8 | 15-21 jun | Reputación + UX + tipos de sede | ✅ COMPLETADO |
| S9 | 22-28 jun | QA integral | ✅ COMPLETADO |
| **S10** | **29-jun/6-jul** | **Panel de pagos + métricas por sede + analítica** | **✅ COMPLETADO** |
| **S11** | **7-11 jul** | **Preparación y defensa (evaluación final, 40%)** | **🟡 en curso** |

---

## 2. MVP: 100% — todo desplegado en producción

---

## 3. Sprint 9 — QA integral ✅

- Suite de pruebas con Vitest: componentes (`CartPage`, `PaymentSuccessPage`) y pruebas
  basadas en propiedades (`fast-check`) para las reglas críticas de reembolso y autorización.
- `npm run lint` y `npm run build` verdes.
- RLS revisada en todas las tablas vía `get_advisors` (seguridad + performance).
- Cierre de las 22 historias de usuario con verificación manual (ver
  `07-Plan-de-Pruebas.md`).

---

## 4. Sprint 10 — Panel de pagos, métricas y analítica ✅

Trabajo desplegado y documentado en detalle en `11-Mejoras-Incorporadas.md`:

- **Panel de admin de pagos:** giros manuales a profesores agrupados por ciclo de corte
  mensual, reembolsos fallidos (reintentar/marcar resuelto), y **margen real** (comisión
  cobrada vs. costo real de MercadoPago por transacción).
- **Métricas de rendimiento reales (M1-M5):** clicleables desde el dashboard admin, con
  explicación humanizada y desglose por sede. M1 se calcula contra la capacidad física de la
  sala; M4 (disponibilidad) con un latido interno cada 5 minutos, sin depender de un servicio
  externo.
- **Corrección del flujo de asistencia:** "pasar lista" ahora marca a todos presentes por
  defecto (el profesor solo desmarca ausentes), corrigiendo un bug que impedía guardar la
  asistencia.
- **Google Analytics 4:** tracking del sitio + sección de comportamiento (usuarios activos,
  sesiones, vistas, páginas más visitadas) integrada en el dashboard admin.
- **Corrección de fee real de MercadoPago:** el webhook de pagos ahora captura la comisión
  real cobrada por MercadoPago en cada transacción.

---

## 5. Sprint 11 — Preparación y defensa (7-11 jul) 🟡

- Informe final (Word) con los 17 puntos de la rúbrica.
- Presentación (PPT) con formato institucional para la defensa.
- Ensayo de la exposición (30 min presentación + 10 min preguntas), demo en vivo del
  software.
- Software verificado 100% funcional en producción antes de la defensa.

---

## 6. Hitos

| # | Fecha | Descripción | Estado |
|---|-------|-------------|--------|
| H0 | 25-abr | Entrega de documentación inicial | ✅ |
| H1 | 3-may | Sistema desplegado | ✅ |
| H2 | 17-may | Búsqueda y carrito | ✅ |
| H3 | 24-may | Pagos consolidados | ✅ |
| H4 | 7-jun | Reagendamiento | ✅ |
| H5 | 14-jun | Panel admin + notificaciones | ✅ |
| H6 | 21-jun | Reputación + tipos de sede | ✅ |
| H7 | 28-jun | QA integral completo | ✅ |
| H8 | 6-jul | Panel de pagos, métricas por sede y analítica | ✅ |
| H9 | 11-jul | Evaluación final (40%) | 🟡 |

---

## 7. Funcionalidades: 40+ implementadas (fuera del alcance original)

---

## 8. Ramas y control de versiones

| Rama | Uso |
|------|-----|
| `main` | Rama de producción — Vercel despliega automáticamente cada push |
| `darllete` | Rama de trabajo activa, se integra a `main` |
| `victor` | Rama de trabajo del integrante |

---

*Elaborado por el equipo. Actualizado al 09 de julio de 2026.*
