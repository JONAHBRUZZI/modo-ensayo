# Carta Gantt — Modo Ensayo
## Actualizada al 13 de junio de 2026

**Equipo:** Darlette Morales · Jonathan Guerra · Victor Silva  
**Asignatura:** Taller Aplicado de Programación - TPY1101  
**Profesor:** Felipe Arturo Castillo Ducaud  

---

## 1. Estado General

| Sprint | Semana | Foco | Estado |
|--------|--------|------|--------|
| S0 | 22-25 abr | Documentación Diseño | ✅ COMPLETADO |
| S1 | 27-abr/3-may | Setup + Auth + JWT | ✅ COMPLETADO |
| S2 | 4-10 may | Usuarios + Sedes + Docs | ✅ COMPLETADO |
| S3 | 11-17 may | Clases + Búsqueda + Carrito | ✅ COMPLETADO |
| S4 | 18-24 may | Pago consolidado + EXP.2 | ✅ COMPLETADO |
| S5 | 25-31 may | Validación Clases + Liberación | ✅ COMPLETADO |
| S6 | 1-7 jun | Reagendamiento + Timeout 48h | ✅ COMPLETADO |
| **S7** | **8-14 jun** | **Admin General + Notificaciones** | **✅ COMPLETADO (13-jun)** |
| **S8** | **15-21 jun** | **Reputación + UX + Tipos Sede** | **✅ COMPLETADO ADELANTADO** |
| **S9** | **22-28 jun** | **QA Integral** | **🟡 70% (JaCoCo + tests listos)** |
| S10 | 29-jun/5-jul | Preparación Defensa | ⬜ Pendiente |
| S11 | 6-12 jul | EVALUACIÓN FINAL (40%) | ⬜ Pendiente |

---

## 2. MVP Completado: ~92%

## 3. Sprints 7 y 8 — Completados al 13-jun

### Sprint 7 — Admin + Notificaciones ✅
- Panel Admin con charts Chart.js (Pie, Bar, Line, Area)
- NotificationBell.vue integrado en navbar con dropdown y contador
- Notificaciones en PaymentService (checkout, pago liberado)
- Notificaciones en VenueService (registro, rechazo, suspensión)
- Notificaciones en AdminService (aprobar/rechazar identidad y sede)
- Fix puedeVerContextoProfesor con hasRoleTeacher
- ConflictException 409 para RUT/correo duplicados

### Sprint 8 — Reputación + Tipos Sede + Equipamiento ✅
- TipoDocumentoSede con 13 valores R22
- VenueDocUpload con tipos dinámicos SEDE/HOME_STUDIO
- EstadoProfesorBadge (ACTIVO/DORMIDO/INACTIVO)
- BorradorSelector.vue reutilizable
- TipoPiso enum en Room + todos los checkboxes de equipamiento
- Badge score ★ en Dashboard Profesor y Configuración Sede
- Endpoint GET /reviews/target/{type}/{id}
- Semilla 55 reviews de prueba
- Validación documentos requeridos por tipo de sede
- Google Sign-In + registro con validaciones completas
- Autocomplete direcciones con Nominatim (OpenStreetMap)
- Toast system reutilizable (reemplaza todos los alert())

---

## 4. Sprint 9 — QA Integral (22-28 jun)

### Completado adelantado:
- JaCoCo configurado en pom.xml con meta 10%
- AdminIntegrationTest + AuthIntegrationTest con asserts 401/403
- 32 tests unitarios en 7 archivos
- Loading states en VenueRegistrationPage
- 55 reviews de semilla para pruebas

### Pendiente:
- Medir cobertura JaCoCo real y subir al 60%
- Pruebas de carga JMeter 50 usuarios concurrentes
- Revisión responsive en 3 dispositivos reales

---

## 5. Sprint 10 — Defensa (29-jun a 5-jul) ⬜

- Video demo 3 minutos
- PPT Evaluación Final (40 min)
- 30 preguntas anticipadas con respuestas
- 2 ensayos formales
- Scripts BD finales documentados

---

## 6. Hitos

| # | Fecha | Descripción | Estado |
|---|-------|-------------|--------|
| H0 | 25-abr | Entrega documentación | ✅ |
| H1 | 3-may | Sistema desplegado | ✅ |
| H2 | 17-may | Búsqueda y carrito | ✅ |
| H3 | Sem 11-12 | EXP. 2 (35%) | ✅ |
| H4 | 7-jun | Reagendamiento | ✅ |
| H5 | 14-jun | Panel Admin + Notificaciones | ✅ |
| H6 | 21-jun | Tipos sede + Docs SII | ✅ Adelantado |
| H7 | 28-jun | QA completo | 🟡 |
| H8 | Jul | EVALUACIÓN FINAL (40%) | ⬜ |

---

## 7. Funcionalidades Extra: 30+ implementadas

## 8. Merge rama darllete: completado 13-jun
- DDL 4 tablas nuevas + seed 5 sedes musicales
- SearchController + AgendaService + ValidacionService

---

## 9. Ramas

| Rama | Estado |
|------|--------|
| main | Sincronizada con victor |
| victor | Rama activa de desarrollo |
| darllete | Mergeada a victor/main |

---

*Elaborado por Victor Silva. Actualizado al 13 de junio de 2026.*
