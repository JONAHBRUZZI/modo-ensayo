# 👨‍💻 ASIGNACIÓN DE EQUIPOS Y RESPONSABILIDADES

## ESTRUCTURA DEL EQUIPO (3 Developers)

---

## 👤 DEVELOPER 1: Full Stack Lead + Arquitecto

**Rol:** Liderazgo técnico, decisiones arquitectura, integración crítica

### Responsabilidades Principales

#### Backend (60% del tiempo)
- **Autenticación & Seguridad**
  - JWT tokens, refresh tokens, logout
  - Encriptación de passwords
  - CORS y CSRF
  - Role-Based Access Control (RBAC)

- **Usuarios & Perfiles**
  - Creación de usuarios
  - Edición de perfiles
  - Verificación de identidad
  - Sistema de roles

- **Pagos (Crítico)**
  - Integración Mercado Pago
  - Webhooks de pagos
  - Refunds
  - Auditoría de transacciones

- **API Global**
  - Error handling centralizado
  - Response formatting estándar
  - Rate limiting
  - Validaciones DTOs

#### Frontend (30% del tiempo)
- Integración servicios críticos
- Login/Register flow
- Testing hooks principales
- Performance

#### DevOps/Infra (10% del tiempo)
- Docker Compose
- Scripts deployment
- Variables de entorno
- CI/CD setup

### Branching Strategy
```
feature/auth-*                 → Auth improvements
feature/payment-*              → Payment features
feature/users-*                → User management
hotfix/*                       → Production fixes
```

### Code Review
- ✅ Aprueba PRs de Dev2 y Dev3
- ✅ Revisa cambios críticos
- ✅ Valida arquitectura

### Reuniones
- Daily standup (9:00 AM)
- Weekly architecture review (viernes)
- Code review sessions (martes/jueves)

---

## 👤 DEVELOPER 2: Backend Specialist

**Rol:** Lógica de negocio, módulos principales, tests backend

### Responsabilidades Principales

#### Backend (90% del tiempo)
- **Gestión de Clases** (CORE)
  - CRUD de clases
  - Validaciones de negocio
  - Estados de clase
  - Filtrado avanzado
  - Paginación

- **Sedes y Salas**
  - Registro de sedes
  - Creación de salas
  - Características de salas
  - Disponibilidad

- **Asistencia**
  - Registro de asistencia
  - Validaciones asistencia
  - Reportes asistencia

- **Reseñas & Calificaciones**
  - CRUD reseñas
  - Cálculo promedio
  - Validaciones

- **Reagendamiento**
  - Lógica de reagendamiento
  - Validaciones de disponibilidad
  - Notificaciones

- **Miembros Asociados**
  - Agregar miembros familia
  - Autorización acceso
  - Gestión de permisos

- **Tests Backend**
  - Tests unitarios (>80% coverage)
  - Tests integración
  - Fixtures de datos

#### Frontend (10% del tiempo)
- Pruebas locales de integraciones
- Feedback sobre APIs

### Base de Datos
- Migraciones schema
- Seeding de datos
- Backups y testing BD

### Branching Strategy
```
feature/classes-*              → Class features
feature/venues-*               → Venue features
feature/attendance-*           → Attendance features
feature/reviews-*              → Review features
feature/reschedules-*          → Reschedule features
feature/associates-*           → Associate features
```

### Tareas Pendientes (Priority List)
1. [ ] Tests unitarios de clases
2. [ ] Tests integración venues
3. [ ] Validaciones de negocio completas
4. [ ] Filtros avanzados clases
5. [ ] Reportes de asistencia

---

## 👤 DEVELOPER 3: Frontend Specialist + QA

**Rol:** UX/UI, componentes frontend, tests frontend, quality assurance

### Responsabilidades Principales

#### Frontend (85% del tiempo)
- **Componentes Base**
  - Button, Input, Card
  - Modal, Dropdown, Navbar
  - Tables, Forms
  - Loading states, Errores

- **Páginas Principales**
  - HomePage
  - ClassesPage (búsqueda, filtros)
  - CartPage (carrito de compras)
  - ProfilePage (edición de perfil)
  - DashboardPages (por rol)

- **Páginas por Rol**
  - Alumno: clases, carrito, pagos, reseñas
  - Profesor: mis clases, métricas, asistencia
  - Admin: usuarios, verificaciones, estadísticas
  - Sede: administración general

- **Funcionalidades**
  - Autenticación UI (login, register)
  - Notificaciones
  - Filtros y búsqueda
  - Paginación
  - Responsive design (mobile first)

- **Testing Frontend**
  - Tests componentes (Vitest)
  - Tests de integración
  - E2E tests

- **Estilos**
  - Tailwind CSS consistente
  - Responsive design
  - Dark mode (si aplica)
  - Accesibilidad (WCAG)

#### QA & Testing (15% del tiempo)
- Pruebas manuales
- Validación flujos
- Testing en múltiples navegadores
- Performance testing
- Reporte de bugs

### Branching Strategy
```
feature/pages-*                → Page features
feature/components-*           → Component features
feature/styling-*              → Style improvements
feature/notifications-*        → Notification features
feature/responsive-*           → Responsive fixes
```

### Checklist Antes de Deploy
```
[ ] Responsive en mobile, tablet, desktop
[ ] Sin console errors/warnings
[ ] Load time < 3s
[ ] Accesibilidad checkeada
[ ] Tests coverage > 80%
[ ] Cross-browser testing (Chrome, Firefox, Safari)
[ ] Performance OK (Lighthouse > 85)
```

### Tareas Pendientes
1. [ ] Componente ErrorBoundary
2. [ ] Estados de loading completos
3. [ ] Tests useAuth hook
4. [ ] Componente filtros avanzados
5. [ ] Responsive tables

---

## 📊 MATRIZ DE RESPONSABILIDADES

| Tarea | Dev1 | Dev2 | Dev3 |
|-------|------|------|------|
| **Backend** | ⭐ (Crítico) | ⭐⭐⭐ (Principal) | • |
| **Frontend** | • | • | ⭐⭐⭐ (Principal) |
| **Tests Backend** | ⭐ | ⭐⭐⭐ | • |
| **Tests Frontend** | • | • | ⭐⭐⭐ |
| **Infra/DevOps** | ⭐⭐ | • | • |
| **Code Review** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **Architecture** | ⭐⭐⭐ | ⭐ | • |
| **QA** | ⭐ | ⭐ | ⭐⭐⭐ |
| **Docs** | ⭐⭐ | ⭐ | ⭐ |

**Leyenda:**
- ⭐⭐⭐ = Responsable principal
- ⭐⭐ = Co-responsable
- ⭐ = Contribuye
- • = Minimal involvement

---

## 🤝 COLABORACIONES REQUERIDAS

### Dev1 ↔ Dev2 (Semanal)
**Tema:** API Backend + Validaciones
- Diseño DTOs
- Endpoints nuevos
- Validaciones de negocio
- Documentación API

### Dev2 ↔ Dev3 (Semanal)
**Tema:** Integraciones Frontend-Backend
- Pruebas servicios
- Debugging API calls
- Feedback de errors
- Datos de testing

### Dev1 ↔ Dev3 (Semanal)
**Tema:** Performance + Seguridad
- Load testing
- XSS/CSRF prevention
- Token management
- Optimizaciones

### Todos (Diarios)
- **09:00 AM:** Standup (15 min)
- **14:00 PM:** Sync si es necesario

---

## 📈 MÉTRICAS POR DEVELOPER

### Dev1 (Lead)
- PRs aprobadas/semana: 10+
- Code review turnaround: < 24h
- Build status: 100% pass
- Commits/semana: 5-8

### Dev2 (Backend)
- Features completadas/semana: 2-3
- Test coverage: > 85%
- Bugs encontrados en testing: < 2
- Commits/semana: 15-20

### Dev3 (Frontend)
- Componentes nuevos/semana: 2-3
- Test coverage: > 80%
- Performance regression: 0
- Commits/semana: 15-20

---

## 🎓 CAPACITACIÓN Y CROSS-TRAINING

### Mes 1
- Dev1 enseña a Dev2: Arquitectura backend
- Dev3 enseña a Dev2: Setup frontend local
- Dev2 enseña a Dev3: Endpoints principales

### Mes 2
- Dev1 enseña a Dev3: Security & Performance
- Dev2 enseña a Dev1: Lógica negocio
- Dev3 enseña a Dev1: UX/UI feedback

### Mes 3
- Cualquier dev puede hacer tareas de otros (backup)

---

## 🚨 ESCALONAMIENTO DE PROBLEMAS

### Bloqueante (Resolver HOY)
1. Aviso inmediato (Slack/Teams)
2. Dev1 prioriza
3. Help de otros si es necesario
4. Update cada 2 horas

### Crítico (Resolver ESTA SEMANA)
1. Crear issue en GitHub
2. Asignar a dev responsable
3. Update en daily standup
4. Review 2x por semana

### Normal (Resolver PRÓXIMAS 2 SEMANAS)
1. Backlog planning
2. Asignar en sprint planning
3. Review en weekly

---

## ✅ DEFINICIÓN DE HECHO (POR ROL)

### Dev1 (Backend Lead)
- [ ] Código escrito y testeado
- [ ] Code review aprobado
- [ ] Documentación actualizada
- [ ] Desplegable a producción

### Dev2 (Backend)
- [ ] Lógica de negocio correcta
- [ ] Tests > 85% coverage
- [ ] API spec cumple
- [ ] Documentación inline

### Dev3 (Frontend)
- [ ] UI responsiva y accesible
- [ ] Tests > 80% coverage
- [ ] Performance OK
- [ ] Storybook updated

---

## 📞 CONTACTO DE EMERGENCIA

| Dev | Slack | Email | GitHub |
|-----|-------|-------|--------|
| Dev1 (Lead) | @dev1 | dev1@email | @dev1 |
| Dev2 (Backend) | @dev2 | dev2@email | @dev2 |
| Dev3 (Frontend) | @dev3 | dev3@email | @dev3 |

**Response time goals:**
- Bloqueante: < 1h
- Crítico: < 4h
- Normal: < 24h

---

**Última actualización:** Mayo 9, 2026
**Próxima revisión:** Mayo 23, 2026
