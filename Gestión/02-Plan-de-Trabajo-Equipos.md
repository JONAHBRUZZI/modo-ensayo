# 📋 PLAN DE TRABAJO - MODO ENSAYO
## Para 3 Desarrolladores | Mayo 2026

---

## 🎯 VISIÓN DEL PROYECTO

**Objetivo:** Plataforma para gestión de clases artísticas con pagos condicionados.

**Estado Actual:** 
- ✅ Frontend: Vue 3 (21 páginas, routing completo)
- ✅ Backend: Spring Boot (12 módulos)
- ✅ DB: PostgreSQL (dockerizada)
- 🔄 Fase: Desarrollo activo (PR #2 abierto)

---

## 🏗️ ARQUITECTURA DEL PROYECTO

### Frontend (Vue 3 + Vite)
```
frontend/src/
├── pages/          → 21 páginas por rol (Alumno, Profesor, Admin, Sede)
├── components/     → Componentes reutilizables
├── services/       → Servicios API (Axios)
├── router/         → Rutas con guards JWT
├── hooks/          → Custom hooks (useAuth, useNotifications)
└── layouts/        → DefaultLayout (navbar + sidebar)
```

### Backend (Spring Boot)
```
backend/src/main/java/com/modoensayo/
├── admin/          → Verificaciones, estadísticas
├── associates/     → Gestión de miembros familia
├── attendance/     → Asistencia a clases
├── auth/           → Login, JWT, registro
├── classes/        → Creación y gestión de clases
├── notifications/  → Sistema de notificaciones
├── payments/       → Integración Mercado Pago
├── reschedules/    → Reagendamiento de clases
├── reviews/        → Reseñas y calificaciones
├── users/          → Perfiles de usuario
├── venues/         → Sedes y salas
└── shared/         → Config global, seguridad
```

---

## 👥 ASIGNACIÓN DE EQUIPOS (3 Developers)

### **Developer 1: Full Stack Lead** 
**Responsable:** Features principales + Code Review
- 🔴 Backend: Módulos críticos (auth, payments, users)
- 🟠 Frontend: Integración de servicios
- 📋 Gestión de PRs
- 🎯 Prioridad: Features backend

### **Developer 2: Backend Specialist**
**Responsable:** Módulos de negocio
- 🟦 Backend: Módulos domain (classes, venues, reviews, reschedules)
- 🟦 Tests backend
- 📊 Migraciones BD
- 🎯 Prioridad: Lógica de negocio

### **Developer 3: Frontend Specialist**
**Responsable:** UI/UX e integraciones frontend
- 🟩 Frontend: Nuevas páginas y componentes
- 🟩 Tests frontend (Vitest)
- 🎨 Tailwind CSS + responsive
- 🎯 Prioridad: Experiencia de usuario

---

## 🌳 ESTRATEGIA DE RAMAS

### Branch Naming Convention
```
feature/                   → Nuevas funcionalidades
├── feature/auth-token-refresh
├── feature/payment-webhook
└── feature/venue-filtering

bugfix/                    → Correcciones
├── bugfix/login-validation
└── bugfix/class-filtering

hotfix/                    → Urgentes en production
├── hotfix/payment-error
└── hotfix/security-patch

refactor/                  → Mejoras código
├── refactor/auth-service
└── refactor/api-response

docs/                      → Documentación
├── docs/api-endpoints
└── docs/setup-guide
```

### Protected Branches
```
main                       → Production ready (required reviews: 2)
dev                        → Integration branch (required reviews: 1)
```

### Workflow Git
```
1. Checkout feature branch desde dev
2. Commit diarios con mensajes descriptivos
3. Push a remote
4. Crear PR cuando esté listo
5. Code review (Dev1 + Dev2/Dev3)
6. Merge a dev
7. QA antes de merge a main
```

---

## 📋 TAREAS PRIORIZADAS (Próximas 2 Semanas)

### FASE 1: Stabilización (Semana 1)
**Lead: Developer 1**

#### Backend
- [ ] ✅ Tests unitarios auth (Dev1)
- [ ] ✅ Tests integración payments (Dev2)
- [ ] ✅ Validación DTOs (Dev1)
- [ ] ✅ Manejo errores global (Dev1)

#### Frontend
- [ ] ✅ Componentes de error (Dev3)
- [ ] ✅ Loading states (Dev3)
- [ ] ✅ Tests useAuth hook (Dev3)
- [ ] ✅ Validación formularios (Dev3)

#### Infra
- [ ] ✅ Docker Compose optimizado (Dev1)
- [ ] ✅ Variables de entorno (.env.example) (Dev1)
- [ ] ✅ Scripts de inicialización DB (Dev2)

---

### FASE 2: Features (Semana 2)
**Lead: Developer 2**

#### Backend
- [ ] 🔨 Webhook Mercado Pago (Dev1 + Dev2)
- [ ] 🔨 Reporte de clases (Dev2)
- [ ] 🔨 Filtros avanzados venues (Dev2)
- [ ] 🔨 Notificaciones push (Dev2)

#### Frontend
- [ ] 🔨 Dashboard métricas (Dev3)
- [ ] 🔨 Filtros y búsqueda (Dev3)
- [ ] 🔨 Notificaciones en tiempo real (Dev3)
- [ ] 🔨 Calendarios (Dev3)

#### Testing
- [ ] 🔨 Integration tests e2e (Dev3)
- [ ] 🔨 Performance tests (Dev1)

---

## 📂 CONVENCIÓN DE COMMITS

### Formato
```
<tipo>(<scope>): <descripción>

<detalles adicionales si es necesario>

Fixes #<issue-number>
```

### Tipos
```
feat:      Nueva funcionalidad
fix:       Corrección de bug
refactor:  Cambio de código sin funcionalidad
style:     Cambios que no afectan código (formatting)
test:      Agregar o actualizar tests
docs:      Cambios en documentación
chore:     Cambios en build, dependencies, etc.
```

### Ejemplos
```
feat(auth): agregar refresh token automático
fix(payments): corregir cálculo de comisión
test(classes): agregar tests para filtrado
docs(setup): actualizar instrucciones instalación
```

---

## 🔄 CODE REVIEW PROCESS

### Para PRs normales
1. Author: Crea PR con descripción detallada
2. Reviewer 1: Revisa código (Dev2 o Dev3)
3. Reviewer 2: Valida tests (Dev1)
4. Author: Aplica cambios
5. Merge a dev

### Para PRs urgentes (hotfix)
1. Author: Crea PR con `hotfix/` prefix
2. Reviewer 1: Revisa urgentemente (Dev1)
3. Reviewer 2: Valida (otro dev)
4. Merge directo a main
5. Cherry-pick a dev

### Checklist para Reviewer
```
[ ] Código sigue convenciones del proyecto
[ ] Tests unitarios incluidos (>80% coverage)
[ ] Sin conflictos con main/dev
[ ] Comentarios explicativos donde sea complejo
[ ] Mensajes de error útiles
[ ] Validaciones de entrada
[ ] Performance OK (sin N+1 queries, etc)
[ ] Documentación actualizada si aplica
```

---

## 📊 MÉTRICAS Y SEGUIMIENTO

### Daily Standup (Reunión diaria - 15 min)
**Horario:** 9:00 AM

**Cada dev comenta:**
1. ¿Qué hice ayer?
2. ¿Qué haré hoy?
3. ¿Bloqueantes o ayuda necesaria?

### Weekly Sprint Review (Viernes - 1 hora)
**Punto:** Demostración de features completadas

### Tracking
- Issues en GitHub
- Milestones por sprint (2 semanas)
- Project board (Kanban)

---

## 🛠️ DESARROLLO LOCAL

### Setup Inicial (Todo dev)
```bash
# 1. Clonar repo
git clone https://github.com/JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo

# 2. Crear rama local
git checkout dev
git pull origin dev
git checkout -b feature/mi-feature

# 3. Backend
cd backend
# Instalar Maven (si no está)
./mvnw install

# 4. Frontend
cd ../frontend
npm install

# 5. Docker
docker compose up -d postgres pgadmin

# 6. Iniciar dev
# Terminal 1: Backend
cd backend && ./mvnw spring-boot:run

# Terminal 2: Frontend
cd frontend && npm run dev
```

### URLs locales
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- pgAdmin: `http://localhost:5050`
- API: `http://localhost:8080/api`

---

## 📚 DOCUMENTACIÓN IMPORTANTE

### Archivos críticos
- `backend/pom.xml` → Dependencias Maven
- `frontend/package.json` → Scripts y dependencias npm
- `infra/postgres/init/` → Scripts inicialización BD
- `docker-compose.yml` → Configuración docker
- `.env.example` → Variables de entorno

### Documentación existente
- `docs/PRD.md` → Requerimientos
- `docs/architecture.md` → Arquitectura
- `docs/database-schema.md` → Schema BD
- `docs/api-endpoints.md` → Endpoints disponibles
- `docs/business-rules.md` → Reglas de negocio

---

## 🚨 ISSUES CONOCIDOS

### Backend
- ⚠️ Maven requiere instalación en Windows
- ⚠️ JWT refresh token aún no implementado

### Frontend
- ⚠️ Notificaciones en tiempo real sin implementar
- ⚠️ Calendarios sin implementar completamente

### DevOps
- ⚠️ Terraform AWS aún no configurado

---

## 📞 COMUNICACIÓN

### Canales
- **Slack/Teams:** Comunicación general
- **GitHub Issues:** Tareas técnicas
- **GitHub PR Comments:** Code review
- **Daily Standup:** 9:00 AM

### Escalación
1. **Bloqueante:** Aviso inmediato al Dev1 (Lead)
2. **Bug crítico:** PR urgente + notificación grupo
3. **Decision arquitectura:** Meeting con todos

---

## ✅ CHECKLIST ANTES DE MERGEAR A MAIN

```
Backend
[ ] Tests unitarios (>80% coverage)
[ ] Integración API completa
[ ] Validaciones de entrada
[ ] Manejo de errores
[ ] Logs apropiados
[ ] Documentación código

Frontend
[ ] Tests componentes
[ ] Responsive design (mobile, tablet, desktop)
[ ] Accesibilidad (WCAG)
[ ] Performance (<3s load)
[ ] Sin console errors/warnings
[ ] Documentación componentes

General
[ ] Code review aprobado (2)
[ ] Commits con mensaje claro
[ ] PR description completa
[ ] Sin conflictos con main
[ ] QA passou
```

---

## 🎯 ROADMAP (Próximos 2 Meses)

```
Semana 1-2:  Stabilización + Fixes
Semana 3-4:  Features core
Semana 5-6:  Integraciones (Mercado Pago, notificaciones)
Semana 7-8:  Performance + Security + Deployment
```

---

## 📝 NOTAS FINALES

1. **Rama Claude:** Usar para features experimentales, mergear a dev cuando esté validada
2. **Code Style:** ESLint + Prettier (frontend), checkstyle (backend)
3. **Testing:** Mínimo 80% coverage en features críticas
4. **Deployment:** Siempre hacer desde main, nunca desde dev
5. **Backup:** Hacer backup BD antes de cambios schema

---

**Última actualización:** Mayo 9, 2026
**Próxima revisión:** Mayo 23, 2026
